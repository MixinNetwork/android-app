package one.mixin.android.webrtc

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Binder
import android.os.IBinder
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.gson.Gson
import dagger.android.AndroidInjection
import io.reactivex.disposables.Disposable
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.MixinApplication
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.supportsOreo
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendMessageJob
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.ui.call.CallNotificationBuilder
import one.mixin.android.ui.call.CallNotificationBuilder.Companion.TYPE_ESTABLISHED
import one.mixin.android.ui.call.CallNotificationBuilder.Companion.TYPE_INCOMING_RINGING
import one.mixin.android.ui.call.CallNotificationBuilder.Companion.TYPE_OUTGOING_RINGING
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Sdp
import one.mixin.android.vo.User
import one.mixin.android.vo.createCallMessage
import one.mixin.android.vo.toUser
import one.mixin.android.webrtc.receiver.IncomingCallReceiver
import one.mixin.android.webrtc.receiver.ScreenOffReceiver
import one.mixin.android.websocket.BlazeMessageData
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.StatsReport
import org.webrtc.VideoCapturer
import org.webrtc.VideoSink
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CallService : Service(), PeerConnectionClient.PeerConnectionEvents {

    private val callExecutor = Executors.newSingleThreadExecutor()
    private val timeoutExecutor = Executors.newScheduledThreadPool(1)
    private var timeoutFuture: ScheduledFuture<*>? = null

    var callback: CallServiceCallback? = null
    var callState = CallState.STATE_IDLE
    private var audioManager: AppRTCAudioManager? = null
    private var audioEnable = true
    private var eglBase: EglBase? = null
    private var videoCapturer: VideoCapturer? = null
    private var localSink: VideoSink? = null
    private var remoteSink: VideoSink? = null

    private var callReceiver: IncomingCallReceiver? = null
    private var screenOffReceiver: ScreenOffReceiver? = null
    private var disposable: Disposable? = null

    private val callBinder = CallBinder()

    @Inject
    lateinit var jobManager: MixinJobManager
    private val peerConnectionClient: PeerConnectionClient by lazy {
        PeerConnectionClient(this, this)
    }

    private val gson = Gson()
    private var blazeMessageData: BlazeMessageData? = null
    private var quoteMessageId: String? = null
    private var self = Session.getAccount()!!.toUser()
    private var user: User? = null
    private var conversationId: String? = null
    private var ringtone: Ringtone? = null
    private var callConnectedTime: Long? = null

    inner class CallBinder : Binder() {
        fun getService(): CallService {
            return this@CallService
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return callBinder
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        peerConnectionClient.createPeerConnectionFactory(PeerConnectionFactory.Options())

        audioManager = AppRTCAudioManager.create(this)
        audioManager!!.start { selectedAudioDevice, availableAudioDevices ->
            Timber.d("onAudioManagerDevicesChanged  selectedAudioDevice: $selectedAudioDevice, availableAudioDevices: $availableAudioDevices")
        }

        callReceiver = IncomingCallReceiver()
        registerReceiver(callReceiver, IntentFilter("android.intent.action.PHONE_STATE"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == null) return START_NOT_STICKY

        callExecutor.execute {
            when (intent.action) {
                ACTION_CALL_INCOMING -> handleCallIncoming(intent)
                ACTION_CALL_OUTGOING -> if (isIdle()) handleCallOutgoing(intent)
                ACTION_CALL_ANSWER -> handleAnswerCall(intent)
                ACTION_CANDIDATE -> handleCandidate(intent)
                ACTION_CALL_CANCEL -> handleCallCancel()
                ACTION_CALL_DECLINE -> handleCallDecline()
                ACTION_CALL_LOCAL_END -> handleCallLocalEnd()
                ACTION_CALL_REMOTE_END -> handleCallRemoteEnd()
                ACTION_CALL_BUSY -> handleCallBusy()
                ACTION_CALL_LOCAL_FAILED -> handleCallLocalFailed()
                ACTION_CALL_REMOTE_FAILED -> handleCallRemoteFailed()

                ACTION_MUTE_AUDIO -> handleMuteAudio(intent)
                ACTION_SCREEN_OFF -> handleScreenOff(intent)
                ACTION_CHECK_TIMEOUT -> handleCheckTimeout()
                ACTION_SWITCH_CAMERA -> handleSwitchCamera()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        audioManager?.stop()
        audioManager = null
        unregisterReceiver(callReceiver)
    }

    fun setVideo(eglBase: EglBase, videoCapturer: VideoCapturer?, localSink: VideoSink, remoteSink: VideoSink) {
        peerConnectionClient.videoEnable = true
        this.eglBase = eglBase
        this.videoCapturer = videoCapturer
        this.localSink = localSink
        this.remoteSink = remoteSink
        peerConnectionClient.eglBase = eglBase
    }

    private fun disconnect() {
        stopForeground(true)
        stopRingtone()
        unregisterScreenOffReceiver()
        peerConnectionClient.close()
        callState = CallState.STATE_IDLE
        disposable?.dispose()
        CallService.stopService(this@CallService)
    }


    private fun handleCallIncoming(intent: Intent) {
        if (!isIdle() || isBusy()) {
            val category = MessageCategory.WEBRTC_AUDIO_BUSY.name
            sendCallMessage(category)
            return
        }
        callState = CallState.STATE_RINGING
        playRingtone()
        blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
        user = intent.getParcelableExtra(ARGS_USER)
        supportsOreo {
            setCallInProgressNotification(TYPE_INCOMING_RINGING)
        }
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        peerConnectionClient.isInitiator = false
        CallActivity.show(MixinApplication.appContext, user, CallActivity.CallAction.CALL_INCOMING.name)
    }

    private fun handleCallOutgoing(intent: Intent) {
        if (callState != CallState.STATE_IDLE) {
            throw IllegalStateException("Error state before handle outgoing call")
        }
        callState = CallState.STATE_DIALING
        playRingtone()
        user = intent.getParcelableExtra(ARGS_USER)
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        setCallInProgressNotification(TYPE_OUTGOING_RINGING)
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)

        peerConnectionClient.isInitiator = true
        peerConnectionClient.createOffer(videoCapturer, localSink, remoteSink)
    }

    private fun handleAnswerCall(intent: Intent) {
        Log.d("@@@", "handleAnswerCall  callState: $callState")
        callState = CallState.STATE_ANSWERING

        if (peerConnectionClient.isInitiator) {
            blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
            setRemoteSdp(Base64.decode(blazeMessageData!!.data))
        } else {
            quoteMessageId = blazeMessageData!!.messageId
            setRemoteSdp(Base64.decode(blazeMessageData!!.data))
            peerConnectionClient.createAnswer(videoCapturer, localSink, remoteSink)
        }
    }

    private fun handleCandidate(intent: Intent) {
        Log.d("@@@", "handleCandidate callState: $callState")
        val blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
        val json = Base64.decode(blazeMessageData.data)
        val iceCandidate = gson.fromJson(String(json), IceCandidate::class.java)
        peerConnectionClient.addRemoteIceCandidate(iceCandidate)
    }

    private fun handleIceConnected() {
        Log.d("@@@", "handleIceConnected  callState: $callState")
        callState = CallState.STATE_CONNECTED
        stopRingtone()
        timeoutFuture?.cancel(true)
        callConnectedTime = System.currentTimeMillis()
        setCallInProgressNotification(TYPE_ESTABLISHED)
        peerConnectionClient.setAudioEnable(audioEnable)
        peerConnectionClient.enableCommunication()
        registerScreenOffReceiver()
        CallActivity.show(this, user, CallActivity.CallAction.CALL_CONNECTED.name)
    }

    private fun handleCallCancel() {
        Log.d("@@@", "handleCallCancel callState: $callState")
        timeoutFuture?.cancel(true)
        if (peerConnectionClient.isInitiator) {
            val category = MessageCategory.WEBRTC_AUDIO_CANCEL.name
            sendCallMessage(category)
        } else {
            CallActivity.disconnected(this)
        }
        disconnect()
    }

    private fun handleCallDecline() {
        Log.d("@@@", "handleCallDecline callState: $callState")
        timeoutFuture?.cancel(true)
        if (peerConnectionClient.isInitiator) {
            CallActivity.disconnected(this)
        } else {
            val category = MessageCategory.WEBRTC_AUDIO_DECLINE.name
            sendCallMessage(category)
        }
        disconnect()
    }

    private fun handleCallLocalEnd() {
        Log.d("@@@", "handleCallComplete callState: $callState")
        timeoutFuture?.cancel(true)
        val category = MessageCategory.WEBRTC_AUDIO_END.name
        sendCallMessage(category)
        disconnect()
    }

    private fun handleCallRemoteEnd() {
        Log.d("@@@", "handleCallComplete callState: $callState")
        if (isIdle()) return

        timeoutFuture?.cancel(true)
        val category = MessageCategory.WEBRTC_AUDIO_END.name
        sendCallMessage(category)
        CallActivity.disconnected(this)
        disconnect()
    }

    private fun handleCallBusy() {
        Log.d("@@@", "handleCallBusy callState: $callState")
        CallActivity.show(this, action = CallActivity.CallAction.CALL_BUSY.name)
        disconnect()
    }

    private fun handleCallLocalFailed() {
        Log.d("@@@", "handleCallFailed callState: $callState")
        val category = MessageCategory.WEBRTC_AUDIO_FAILED.name
        sendCallMessage(category)
        CallActivity.disconnected(this)
        disconnect()
    }

    private fun handleCallRemoteFailed() {
        Log.d("@@@", "handleCallFailed callState: $callState")
        if (isIdle()) return

        CallActivity.disconnected(this)
        disconnect()
    }

    private fun handleMuteAudio(intent: Intent) {
        Log.d("@@@", "handleMuteAudio  callState: $callState")
        audioEnable = !intent.extras.getBoolean(EXTRA_MUTE)
        peerConnectionClient.setAudioEnable(audioEnable)
    }

    private fun handleCheckTimeout() {
        if (callState == CallState.STATE_CONNECTED) return

        handleCallCancel()
    }

    private fun handleSwitchCamera() {
        peerConnectionClient.switchCamera()
    }

    private fun registerScreenOffReceiver() {
        if (screenOffReceiver == null) {
            screenOffReceiver = ScreenOffReceiver()
            registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        }
    }

    private fun unregisterScreenOffReceiver() {
        if (screenOffReceiver != null) {
            unregisterReceiver(screenOffReceiver)
            screenOffReceiver = null
        }
    }

    private fun setCallInProgressNotification(type: Int) {
        Timber.d("setCallInProgressNotification")
        startForeground(CallNotificationBuilder.WEBRTC_NOTIFICATION,
            CallNotificationBuilder.getCallInProgressNotification(this, type))
    }

    private fun setRemoteSdp(json: ByteArray) {
        val sdp = gson.fromJson(String(json), Sdp::class.java)
        val sessionDescription = SessionDescription(getType(sdp.type), sdp.sdp)
        peerConnectionClient.setRemoteDescription(sessionDescription)
    }

    private fun getType(type: String): SessionDescription.Type {
        return when (type) {
            SessionDescription.Type.OFFER.name -> SessionDescription.Type.OFFER
            SessionDescription.Type.ANSWER.name -> SessionDescription.Type.ANSWER
            SessionDescription.Type.PRANSWER.name -> SessionDescription.Type.PRANSWER
            else -> SessionDescription.Type.OFFER
        }
    }

    private fun playRingtone() {
        if (ringtone == null) {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (uri != null) {
                ringtone = RingtoneManager.getRingtone(this, uri)
            }
        } else if (ringtone!!.isPlaying) {
            return
        }
        ringtone?.play()
    }

    private fun stopRingtone() {
        if (ringtone == null || !ringtone!!.isPlaying) {
            return
        }
        ringtone!!.stop()
    }

    private fun isIncomingMessageExpired(intent: Intent) =
        System.currentTimeMillis() - intent.getLongExtra(EXTRA_TIMESTAMP, -1) > TimeUnit.MINUTES.toMillis(DEFAULT_TIMEOUT_MINUTES)

    private fun handleScreenOff(intent: Intent) {
    }

    private fun isIdle() = callState == CallState.STATE_IDLE

    private fun isBusy(): Boolean {
        val tm = getSystemService<TelephonyManager>()
        return callState != CallState.STATE_IDLE || tm?.callState != TelephonyManager.CALL_STATE_IDLE
    }

    // PeerConnectionEvents
    override fun onLocalDescription(sdp: SessionDescription) {
        Log.d("@@@", "onLocalDescription")
        callExecutor.execute {
            val category = if (peerConnectionClient.isInitiator) {
                MessageCategory.WEBRTC_AUDIO_OFFER.name
            } else {
                MessageCategory.WEBRTC_AUDIO_ANSWER.name
            }
            sendCallMessage(category, gson.toJson(Sdp(sdp.description, sdp.type.name)))
        }
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        Log.d("@@@", "onIceCandidate")
        callExecutor.execute {
            sendCallMessage(MessageCategory.WEBRTC_ICE_CANDIDATE.name, gson.toJson(candidate))
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
    }

    override fun onIceConnected() {
        callExecutor.execute { handleIceConnected() }
    }

    override fun onIceDisconnected() {
        CallService.stopService(this)
    }

    override fun onPeerConnectionClosed() {
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport>) {
    }

    override fun onPeerConnectionError(description: String) {
        Timber.e(description)
    }

    override fun onCameraSwitchDone(isFrontCamera: Boolean) {
        callback?.onCameraSwitchDone(isFrontCamera)
    }

    private fun sendCallMessage(category: String, content: String? = null) {
        val message = if (peerConnectionClient.isInitiator) {
            if (conversationId == null) {
                throw IllegalStateException("Initiator's conversationId can not be null!")
            }
            val messageId = UUID.randomUUID().toString()
            if (category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
                quoteMessageId = messageId
                createCallMessage(messageId, conversationId!!, self.userId, category, content,
                    nowInUtc(), MessageStatus.SENDING)
            } else {
                if (category == MessageCategory.WEBRTC_AUDIO_END.name) {
                    val duration = System.currentTimeMillis() - callConnectedTime!!
                    createCallMessage(messageId, conversationId!!, self.userId, category, content,
                        nowInUtc(), MessageStatus.SENDING, quoteMessageId, duration.toString())
                } else {
                    createCallMessage(messageId, conversationId!!, self.userId, category, content,
                        nowInUtc(), MessageStatus.SENDING, quoteMessageId)
                }
            }
        } else {
            if (blazeMessageData == null) {
                throw IllegalStateException("Answer's blazeMessageData can not be null!")
            }
            if (category == MessageCategory.WEBRTC_AUDIO_END.name) {
                val duration = System.currentTimeMillis() - callConnectedTime!!
                createCallMessage(UUID.randomUUID().toString(), blazeMessageData!!.conversationId,
                    self.userId, category, content, nowInUtc(), MessageStatus.SENDING, quoteMessageId,
                    duration.toString())
            } else {
                createCallMessage(UUID.randomUUID().toString(), blazeMessageData!!.conversationId,
                    self.userId, category, content, nowInUtc(), MessageStatus.SENDING, quoteMessageId)
            }
        }
        val recipientId = when {
            user != null -> user!!.userId
            blazeMessageData != null -> blazeMessageData!!.userId
            else -> null
        }
        Log.d("@@@", "category: $category, quoteMessageId: $quoteMessageId")
        jobManager.addJobInBackground(SendMessageJob(message, recipientId = recipientId))
    }

    private class TimeoutRunnable(private val context: Context) : Runnable {
        override fun run() {
            CallService.startService(context, ACTION_CHECK_TIMEOUT)
        }
    }

    enum class CallState {
        STATE_IDLE, STATE_DIALING, STATE_RINGING, STATE_ANSWERING, STATE_CONNECTED
    }

    interface CallServiceCallback {
        fun onCameraSwitchDone(isFrontCamera: Boolean)
    }

    companion object {
        const val TAG = "CallService"

        const val DEFAULT_TIMEOUT_MINUTES = 1L

        const val ACTION_CALL_INCOMING = "call_incoming"
        const val ACTION_CALL_OUTGOING = "call_outgoing"
        const val ACTION_CALL_ANSWER = "call_answer"
        const val ACTION_CANDIDATE = "candidate"
        const val ACTION_CALL_CANCEL = "call_cancel"
        const val ACTION_CALL_DECLINE = "call_decline"
        const val ACTION_CALL_LOCAL_END = "call_local_end"
        const val ACTION_CALL_REMOTE_END = "call_remote_end"
        const val ACTION_CALL_BUSY = "call_busy"
        const val ACTION_CALL_LOCAL_FAILED = "call_local_failed"
        const val ACTION_CALL_REMOTE_FAILED = "call_remote_failed"

        const val ACTION_SCREEN_OFF = "screen_off"
        const val ACTION_CHECK_TIMEOUT = "check_timeout"
        const val ACTION_MUTE_AUDIO = "mute_audio"
        const val ACTION_SWITCH_CAMERA = "switch_camera"

        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_BLAZE = "blaze"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_MUTE = "mute"

        fun startService(ctx: Context, action: String? = null, putExtra: ((intent: Intent) -> Unit)? = null) {
            val intent = Intent(ctx, CallService::class.java).apply {
                this.action = action
                putExtra?.invoke(this)
            }
            ContextCompat.startForegroundService(ctx, intent)
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, CallService::class.java))
        }
    }
}