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
import io.reactivex.schedulers.Schedulers
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.Constants.Call.INTERVAL_23_HOURS
import one.mixin.android.Constants.Call.PREF_TURN
import one.mixin.android.Constants.Call.PREF_TURN_FETCH
import one.mixin.android.api.service.AccountService
import one.mixin.android.crypto.Base64
import one.mixin.android.db.MessageDao
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.putLong
import one.mixin.android.extension.putString
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendMessageJob
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.ui.call.CallNotificationBuilder
import one.mixin.android.util.ErrorHandler
import one.mixin.android.util.Session
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Sdp
import one.mixin.android.vo.TurnServer
import one.mixin.android.vo.User
import one.mixin.android.vo.createCallMessage
import one.mixin.android.vo.toUser
import one.mixin.android.webrtc.receiver.IncomingCallReceiver
import one.mixin.android.webrtc.receiver.ScreenOffReceiver
import one.mixin.android.websocket.BlazeMessageData
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
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

    private val peerConnectionClient: PeerConnectionClient by lazy {
        PeerConnectionClient(this, this)
    }

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var messageDao: MessageDao
    @Inject
    lateinit var accountService: AccountService
    @Inject
    lateinit var callState: one.mixin.android.vo.CallState

    private val gson = Gson()
    private var blazeMessageData: BlazeMessageData? = null
    private var quoteMessageId: String? = null
    private var self = Session.getAccount()!!.toUser()
    private var user: User? = null
    private var conversationId: String? = null
    private var ringtone: Ringtone? = null
    private val candidateCache = arrayListOf<IceCandidate>()

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
                ACTION_CALL_OUTGOING -> handleCallOutgoing(intent)
                ACTION_CALL_ANSWER -> handleAnswerCall(intent)
                ACTION_CANDIDATE -> handleCandidate(intent)
                ACTION_CALL_CANCEL -> handleCallCancel(intent)
                ACTION_CALL_DECLINE -> handleCallDecline()
                ACTION_CALL_LOCAL_END -> handleCallLocalEnd(intent)
                ACTION_CALL_REMOTE_END -> handleCallRemoteEnd()
                ACTION_CALL_BUSY -> handleCallBusy()
                ACTION_CALL_LOCAL_FAILED -> handleCallLocalFailed()
                ACTION_CALL_REMOTE_FAILED -> handleCallRemoteFailed()

                ACTION_MUTE_AUDIO -> handleMuteAudio(intent)
                ACTION_SPEAKERPHONE -> handleSpeakerphone(intent)
                ACTION_SCREEN_OFF -> handleScreenOff(intent)
                ACTION_CHECK_TIMEOUT -> handleCheckTimeout()
                ACTION_SWITCH_CAMERA -> handleSwitchCamera()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        callState.setCallState(CallState.STATE_IDLE)
        unregisterScreenOffReceiver()
        audioManager?.stop()
        audioManager = null
        unregisterReceiver(callReceiver)
        Log.d("@@@", "CallService onDestroy callState: ${callState.callInfo.callState}")
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
        peerConnectionClient.close()
        disposable?.dispose()
        candidateCache.clear()
        CallService.stopService(this)
    }

    private fun handleCallIncoming(intent: Intent) {
        if (!isIdle() || isBusy()) {
            val category = MessageCategory.WEBRTC_AUDIO_BUSY.name
            sendCallMessage(category)
            return
        }
        callState.setCallState(CallState.STATE_RINGING)
        playRingtone()
        blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
        user = intent.getParcelableExtra(ARGS_USER)
        callState.setUser(user)
        quoteMessageId = blazeMessageData!!.messageId
        updateNotification()
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        peerConnectionClient.isInitiator = false
        CallActivity.show(this, user)
    }

    private fun handleCallOutgoing(intent: Intent) {
        callState.setCallState(CallState.STATE_DIALING)
        playRingtone()
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        user = intent.getParcelableExtra(ARGS_USER)
        callState.setUser(user)
        updateNotification()
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        peerConnectionClient.isInitiator = true
        CallActivity.show(this, user)
        getTurnServer { peerConnectionClient.createOffer(it, videoCapturer, localSink, remoteSink) }
    }

    private fun handleAnswerCall(intent: Intent) {
        Log.d("@@@", "handleAnswerCall  callState: ${callState.callInfo.callState}")
        callState.setCallState(CallState.STATE_ANSWERING)

        if (peerConnectionClient.isInitiator) {
            val bmd = intent.getSerializableExtra(EXTRA_BLAZE) ?: return
            blazeMessageData = bmd as BlazeMessageData
            sendCallMessage(MessageCategory.WEBRTC_ICE_CANDIDATE.name, gson.toJson(candidateCache))
            setRemoteSdp(Base64.decode(blazeMessageData!!.data))
        } else {
            if (blazeMessageData == null) return
            setRemoteSdp(Base64.decode(blazeMessageData!!.data))
            getTurnServer {
                peerConnectionClient.createAnswer(it, videoCapturer, localSink, remoteSink)
            }
        }
    }

    private fun handleCandidate(intent: Intent) {
        Log.d("@@@", "handleCandidate callState: ${callState.callInfo.callState}")
        val blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
        val json = String(Base64.decode(blazeMessageData.data))
        val ices = gson.fromJson(json, Array<IceCandidate>::class.java)
        ices.forEach {
            peerConnectionClient.addRemoteIceCandidate(it)
        }
    }

    private fun handleIceConnected() {
        Log.d("@@@", "handleIceConnected  callState:${callState.callInfo.callState}")
        callState.setCallState(CallState.STATE_CONNECTED)
        stopRingtone()
        timeoutFuture?.cancel(true)
        callState.setConnectedTime(System.currentTimeMillis())
        updateNotification()
        peerConnectionClient.setAudioEnable(audioEnable)
        peerConnectionClient.enableCommunication()
        registerScreenOffReceiver()
    }

    private fun handleCallCancel(intent: Intent? = null) {
        Log.d("@@@", "handleCallCancel callState: ${callState.callInfo.callState}")
        timeoutFuture?.cancel(true)
        if (peerConnectionClient.isInitiator) {
            val category = MessageCategory.WEBRTC_AUDIO_CANCEL.name
            sendCallMessage(category)
            val fromNotification = intent?.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)
            if (fromNotification != null && fromNotification) {
                callState.setCallState(CallState.STATE_IDLE)
            }
        } else {
            callState.setCallState(CallState.STATE_IDLE)
        }
        disconnect()
    }

    private fun handleCallDecline() {
        Log.d("@@@", "handleCallDecline callState: ${callState.callInfo.callState}")
        timeoutFuture?.cancel(true)
        if (peerConnectionClient.isInitiator) {
            callState.setCallState(CallState.STATE_IDLE)
        } else {
            val category = MessageCategory.WEBRTC_AUDIO_DECLINE.name
            sendCallMessage(category)
        }
        disconnect()
    }

    private fun handleCallLocalEnd(intent: Intent? = null) {
        Log.d("@@@", "handleCallLocalEnd callState: ${callState.callInfo.callState}")
        timeoutFuture?.cancel(true)
        val category = MessageCategory.WEBRTC_AUDIO_END.name
        sendCallMessage(category)
        val fromNotification = intent?.getBooleanExtra(EXTRA_FROM_NOTIFICATION, false)
        if (fromNotification != null && fromNotification) {
            callState.setCallState(CallState.STATE_IDLE)
        }
        disconnect()
    }

    private fun handleCallRemoteEnd() {
        Log.d("@@@", "handleCallRemoteEnd callState: ${callState.callInfo.callState}")
        timeoutFuture?.cancel(true)
        if (blazeMessageData != null && blazeMessageData!!.quoteMessageId != null && conversationId != null) {
            val duration = System.currentTimeMillis() - callState.callInfo.connectedTime!!
            val message = createCallMessage(blazeMessageData!!.quoteMessageId!!, conversationId!!,
                self.userId, MessageCategory.WEBRTC_AUDIO_END.name, null,
                nowInUtc(), MessageStatus.DELIVERED, quoteMessageId, duration.toString())
            messageDao.insert(message)
        }
        callState.setCallState(CallState.STATE_IDLE)
        disconnect()
    }

    private fun handleCallBusy() {
        Log.d("@@@", "handleCallBusy callState: ${callState.callInfo.callState}")
        callState.setCallState(CallState.STATE_BUSY)
        disconnect()
    }

    private fun handleCallLocalFailed() {
        Log.d("@@@", "handleCallLocalFailed callState: ${callState.callInfo.callState}")
        val category = MessageCategory.WEBRTC_AUDIO_FAILED.name
        sendCallMessage(category)
        callState.setCallState(CallState.STATE_IDLE)
        disconnect()
    }

    private fun handleCallRemoteFailed() {
        Log.d("@@@", "handleCallRemoteFailed callState: ${callState.callInfo.callState}")
        if (isIdle()) return

        callState.setCallState(CallState.STATE_IDLE)
        disconnect()
    }

    private fun handleMuteAudio(intent: Intent) {
        Log.d("@@@", "handleMuteAudio  callState: ${callState.callInfo.callState}")
        audioEnable = !intent.extras.getBoolean(EXTRA_MUTE)
        peerConnectionClient.setAudioEnable(audioEnable)
    }

    private fun handleSpeakerphone(intent: Intent) {
        Log.d("@@@", "handleSpeakerphone callState: ${callState.callInfo.callState}")
        val speakerphone = intent.extras.getBoolean(EXTRA_SPEAKERPHONE)
        audioManager?.setSavedIsSpeakerPhoneOn(speakerphone)
    }

    private fun handleCheckTimeout() {
        if (callState.callInfo.callState == CallState.STATE_CONNECTED) return

        callState.setCallState(CallState.STATE_IDLE)
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

    private fun updateNotification() {
        Timber.d("updateNotification")
        startForeground(CallNotificationBuilder.WEBRTC_NOTIFICATION,
            CallNotificationBuilder.getCallNotification(this, callState, user?.fullName))
    }

    private fun setRemoteSdp(json: ByteArray) {
        val sdp = gson.fromJson(String(json), Sdp::class.java)
        val sessionDescription = SessionDescription(getType(sdp.type), sdp.sdp)
        Timber.d("setRemoteSdp: $sessionDescription")
        peerConnectionClient.setRemoteDescription(sessionDescription)
    }

    private fun getType(type: String): SessionDescription.Type {
        return when (type) {
            SessionDescription.Type.OFFER.canonicalForm() -> SessionDescription.Type.OFFER
            SessionDescription.Type.ANSWER.canonicalForm() -> SessionDescription.Type.ANSWER
            SessionDescription.Type.PRANSWER.canonicalForm() -> SessionDescription.Type.PRANSWER
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

    private fun handleScreenOff(intent: Intent) {
    }

    private fun isIdle() = callState.callInfo.callState == CallState.STATE_IDLE

    private fun isBusy(): Boolean {
        val tm = getSystemService<TelephonyManager>()
        return callState.callInfo.callState != CallState.STATE_IDLE || tm?.callState != TelephonyManager.CALL_STATE_IDLE
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
            sendCallMessage(category, gson.toJson(Sdp(sdp.description, sdp.type.canonicalForm())))
        }
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        Log.d("@@@", "onIceCandidate")
        callExecutor.execute {
            if (callState.callInfo.callState == CallState.STATE_DIALING ||
                callState.callInfo.callState == CallState.STATE_RINGING ||
                callState.callInfo.callState == CallState.STATE_ANSWERING) {
                candidateCache.add(candidate)
            } else {
                val arr = arrayListOf(candidate)
                sendCallMessage(MessageCategory.WEBRTC_ICE_CANDIDATE.name, gson.toJson(arr))
            }
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
        handleCallLocalFailed()
    }

    override fun onCameraSwitchDone(isFrontCamera: Boolean) {
        callback?.onCameraSwitchDone(isFrontCamera)
    }

    private fun sendCallMessage(category: String, content: String? = null) {
        val message = if (peerConnectionClient.isInitiator) {
            if (conversationId == null) {
                Log.e("@@@", "Initiator's conversationId can not be null!")
                handleCallCancel()
                return
            }
            val messageId = UUID.randomUUID().toString()
            if (category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
                quoteMessageId = messageId
                callState.setMessageId(messageId)
                createCallMessage(messageId, conversationId!!, self.userId, category, content,
                    nowInUtc(), MessageStatus.SENDING)
            } else {
                if (category == MessageCategory.WEBRTC_AUDIO_END.name) {
                    val duration = System.currentTimeMillis() - callState.callInfo.connectedTime!!
                    createCallMessage(messageId, conversationId!!, self.userId, category, content,
                        nowInUtc(), MessageStatus.SENDING, quoteMessageId, duration.toString())
                } else {
                    createCallMessage(messageId, conversationId!!, self.userId, category, content,
                        nowInUtc(), MessageStatus.SENDING, quoteMessageId)
                }
            }
        } else {
            if (blazeMessageData == null) {
                Log.e("@@@", "Answer's blazeMessageData can not be null!")
                handleCallDecline()
                return
            }
            if (category == MessageCategory.WEBRTC_AUDIO_END.name) {
                val duration = System.currentTimeMillis() - callState.callInfo.connectedTime!!
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
        if (quoteMessageId != null || message.category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
            jobManager.addJobInBackground(SendMessageJob(message, recipientId = recipientId))
        }
    }

    private fun getTurnServer(action: (List<PeerConnection.IceServer>) -> Unit) {
        val lastTimeTurn = defaultSharedPreferences.getLong(PREF_TURN_FETCH, 0L)
        val cur = System.currentTimeMillis()
        if (cur - lastTimeTurn < INTERVAL_23_HOURS) {
            val turn = defaultSharedPreferences.getString(PREF_TURN, null)
            if (!turn.isNullOrBlank()) {
                val turnList = gson.fromJson(turn, Array<TurnServer>::class.java)
                action.invoke(genIceServerList(turnList))
                return
            }
        }
        disposable = accountService.getTurn().subscribeOn(Schedulers.io()).subscribe({
            if (it.isSuccess) {
                val array = it.data as Array<TurnServer>
                val string = gson.toJson(array)
                defaultSharedPreferences.putLong(PREF_TURN_FETCH, System.currentTimeMillis())
                defaultSharedPreferences.putString(PREF_TURN, string)
                action.invoke(genIceServerList(array))
            } else {
                handleFetchTurnError()
            }
        }, {
            ErrorHandler.handleError(it)
            handleFetchTurnError()
        })
    }

    private fun handleFetchTurnError() {
        if (peerConnectionClient.isInitiator) {
            handleCallCancel()
        } else {
            handleCallDecline()
        }
    }

    private fun genIceServerList(array: Array<TurnServer>): List<PeerConnection.IceServer> {
        val iceServer = arrayListOf<PeerConnection.IceServer>()
        array.forEach {
            iceServer.add(PeerConnection.IceServer.builder(it.url)
                .setUsername(it.username)
                .setPassword(it.credential)
                .createIceServer())
        }
        return iceServer
    }

    private class TimeoutRunnable(private val context: Context) : Runnable {
        override fun run() {
            CallService.startService(context, ACTION_CHECK_TIMEOUT)
        }
    }

    enum class CallState {
        STATE_IDLE, STATE_DIALING, STATE_RINGING, STATE_ANSWERING, STATE_CONNECTED, STATE_BUSY
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
        const val ACTION_SPEAKERPHONE = "speakerphone"
        const val ACTION_SWITCH_CAMERA = "switch_camera"

        const val EXTRA_FROM_NOTIFICATION = "from_notification"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_BLAZE = "blaze"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_MUTE = "mute"
        const val EXTRA_SPEAKERPHONE = "speakerphone"

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