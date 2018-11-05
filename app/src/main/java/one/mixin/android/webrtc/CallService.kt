package one.mixin.android.webrtc

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import one.mixin.android.extension.vibrate
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
import one.mixin.android.websocket.BlazeMessageData
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.StatsReport
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

    private val audioManager: CallAudioManager by lazy {
        CallAudioManager(this)
    }
    private var audioEnable = true

    private var callReceiver: IncomingCallReceiver? = null
    private var disposable: Disposable? = null

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
    private val candidateCache = arrayListOf<IceCandidate>()
    private var declineTriggeredByUser: Boolean = true

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        peerConnectionClient.createPeerConnectionFactory(PeerConnectionFactory.Options())

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
                ACTION_CHECK_TIMEOUT -> handleCheckTimeout()
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        callState.reset()
        unregisterReceiver(callReceiver)
    }

    private fun disconnect() {
        stopForeground(true)
        audioManager.stop()
        peerConnectionClient.close()
        disposable?.dispose()
        candidateCache.clear()
    }

    private fun handleCallIncoming(intent: Intent) {
        if (!callState.isIdle() || isBusy()) {
            val category = MessageCategory.WEBRTC_AUDIO_BUSY.name
            val bmd = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
            val m = createCallMessage(UUID.randomUUID().toString(), bmd.conversationId, self.userId, category, null,
                nowInUtc(), MessageStatus.SENDING, bmd.messageId)
            jobManager.addJobInBackground(SendMessageJob(m, recipientId = bmd.userId))

            val savedMessage = createCallMessage(bmd.messageId, m.conversationId, bmd.userId, m.category, m.content,
                m.createdAt, MessageStatus.DELIVERED, bmd.messageId)
            messageDao.insert(savedMessage)
            return
        }
        if (callState.callInfo.callState == CallState.STATE_RINGING) return

        callState.setCallState(CallState.STATE_RINGING)
        audioManager.start(false)
        blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
        user = intent.getParcelableExtra(ARGS_USER)
        callState.setUser(user)
        updateNotification()
        quoteMessageId = blazeMessageData!!.messageId
        callState.setMessageId(quoteMessageId!!)
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        peerConnectionClient.isInitiator = false
        callState.setIsInitiator(false)
        CallActivity.show(this, user)
    }

    private fun handleCallOutgoing(intent: Intent) {
        if (callState.callInfo.callState == CallState.STATE_DIALING) return

        callState.setCallState(CallState.STATE_DIALING)
        audioManager.start(true)
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        user = intent.getParcelableExtra(ARGS_USER)
        callState.setUser(user)
        updateNotification()
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        peerConnectionClient.isInitiator = true
        callState.setIsInitiator(true)
        CallActivity.show(this, user)
        getTurnServer { peerConnectionClient.createOffer(it) }
    }

    private fun handleAnswerCall(intent: Intent) {
        if (callState.callInfo.callState == CallState.STATE_ANSWERING) return

        callState.setCallState(CallState.STATE_ANSWERING)
        updateNotification()
        audioManager.stop()

        if (peerConnectionClient.isInitiator) {
            val bmd = intent.getSerializableExtra(EXTRA_BLAZE) ?: return
            blazeMessageData = bmd as BlazeMessageData
            sendCallMessage(MessageCategory.WEBRTC_ICE_CANDIDATE.name, gson.toJson(candidateCache))
            setRemoteSdp(Base64.decode(blazeMessageData!!.data))
        } else {
            if (blazeMessageData == null) return
            setRemoteSdp(Base64.decode(blazeMessageData!!.data))
            getTurnServer {
                peerConnectionClient.createAnswer(it)
            }
        }
    }

    private fun handleCandidate(intent: Intent) {
        val blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
        val json = String(Base64.decode(blazeMessageData.data))
        val ices = gson.fromJson(json, Array<IceCandidate>::class.java)
        ices.forEach {
            peerConnectionClient.addRemoteIceCandidate(it)
        }
        updateNotification()
    }

    private fun handleIceConnected() {
        if (callState.callInfo.callState == CallState.STATE_CONNECTED) return

        callState.setConnectedTime(System.currentTimeMillis())
        callState.setCallState(CallState.STATE_CONNECTED)
        updateNotification()
        timeoutFuture?.cancel(true)
        vibrate(longArrayOf(0, 30))
        peerConnectionClient.setAudioEnable(audioEnable)
        peerConnectionClient.enableCommunication()
    }

    private fun handleCallCancel(intent: Intent? = null) {
        if (callState.callInfo.callState == CallState.STATE_IDLE) return

        timeoutFuture?.cancel(true)
        if (peerConnectionClient.isInitiator) {
            val category = MessageCategory.WEBRTC_AUDIO_CANCEL.name
            sendCallMessage(category)
            val toIdle = intent?.getBooleanExtra(EXTRA_TO_IDLE, false)
            if (toIdle != null && toIdle) {
                callState.setCallState(CallState.STATE_IDLE)
            }
        } else {
            callState.setCallState(CallState.STATE_IDLE)
        }
        updateNotification()
        disconnect()
    }

    private fun handleCallDecline() {
        if (callState.callInfo.callState == CallState.STATE_IDLE) return

        timeoutFuture?.cancel(true)
        if (peerConnectionClient.isInitiator) {
            callState.setCallState(CallState.STATE_IDLE)
        } else {
            val category = MessageCategory.WEBRTC_AUDIO_DECLINE.name
            sendCallMessage(category)
        }
        updateNotification()
        disconnect()
    }

    private fun handleCallLocalEnd(intent: Intent? = null) {
        if (callState.callInfo.callState == CallState.STATE_IDLE) return

        timeoutFuture?.cancel(true)
        val category = MessageCategory.WEBRTC_AUDIO_END.name
        sendCallMessage(category)
        val toIdle = intent?.getBooleanExtra(EXTRA_TO_IDLE, false)
        if (toIdle != null && toIdle) {
            callState.setCallState(CallState.STATE_IDLE)
        }
        updateNotification()
        disconnect()
    }

    private fun handleCallRemoteEnd() {
        if (callState.callInfo.callState == CallState.STATE_IDLE) return

        timeoutFuture?.cancel(true)
        callState.setCallState(CallState.STATE_IDLE)
        updateNotification()
        disconnect()
    }

    private fun handleCallBusy() {
        callState.setCallState(CallState.STATE_BUSY)
        updateNotification()
        disconnect()
    }

    private fun handleCallLocalFailed() {
        val category = MessageCategory.WEBRTC_AUDIO_FAILED.name
        sendCallMessage(category)
        callState.setCallState(CallState.STATE_IDLE)
        updateNotification()
        disconnect()
    }

    private fun handleCallRemoteFailed() {
        callState.setCallState(CallState.STATE_IDLE)
        updateNotification()
        disconnect()
    }

    private fun handleMuteAudio(intent: Intent) {
        audioEnable = !intent.extras.getBoolean(EXTRA_MUTE)
        peerConnectionClient.setAudioEnable(audioEnable)
        updateNotification()
    }

    private fun handleSpeakerphone(intent: Intent) {
        val speakerphone = intent.extras.getBoolean(EXTRA_SPEAKERPHONE)
        audioManager.isSpeakerOn = speakerphone
        updateNotification()
    }

    private fun handleCheckTimeout() {
        if (callState.callInfo.callState == CallState.STATE_CONNECTED) return

        updateNotification()
        handleCallCancel()
    }

    private fun updateNotification() {
        startForeground(CallNotificationBuilder.WEBRTC_NOTIFICATION,
            CallNotificationBuilder.getCallNotification(this, callState, user))
    }

    private fun setRemoteSdp(json: ByteArray) {
        val sdp = gson.fromJson(String(json), Sdp::class.java)
        val sessionDescription = SessionDescription(getType(sdp.type), sdp.sdp)
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

    private fun isBusy(): Boolean {
        val tm = getSystemService<TelephonyManager>()
        return callState.callInfo.callState != CallState.STATE_IDLE || tm?.callState != TelephonyManager.CALL_STATE_IDLE
    }

    // PeerConnectionEvents
    override fun onLocalDescription(sdp: SessionDescription) {
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
    }

    override fun onPeerConnectionClosed() {
        CallService.stopService(this)
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport>) {
    }

    override fun onPeerConnectionError(description: String) {
        Timber.d("onPeerConnectionError: $description")
        //TODO
    }

    private fun sendCallMessage(category: String, content: String? = null) {
        val message = if (peerConnectionClient.isInitiator) {
            if (conversationId == null) {
                Log.e("@@@", "Initiator's conversationId can not be null!")
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
        if (quoteMessageId != null || message.category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
            jobManager.addJobInBackground(SendMessageJob(message, recipientId = recipientId))
        }

        saveMessage(message)
    }

    private fun saveMessage(m: Message) {
        when {
            m.category == MessageCategory.WEBRTC_AUDIO_DECLINE.name -> {
                val status = if (declineTriggeredByUser) MessageStatus.READ else MessageStatus.DELIVERED
                val uId = if (callState.callInfo.isInitiator) {
                    self.userId
                } else {
                    callState.callInfo.user!!.userId
                }
                messageDao.insert(createNewReadMessage(m, uId, status))
            }
            m.category == MessageCategory.WEBRTC_AUDIO_CANCEL.name -> {
                val msg = createCallMessage(m.id, m.conversationId, self.userId, m.category, m.content,
                    m.createdAt, MessageStatus.READ, m.quoteMessageId, m.mediaDuration)
                messageDao.insert(msg)
            }
            m.category == MessageCategory.WEBRTC_AUDIO_END.name || m.category == MessageCategory.WEBRTC_AUDIO_FAILED.name -> {
                val msg = if (callState.callInfo.isInitiator) {
                    createNewReadMessage(m, self.userId, MessageStatus.READ)
                } else {
                    createNewReadMessage(m, callState.callInfo.user!!.userId, MessageStatus.READ)
                }
                messageDao.insert(msg)
            }
        }
    }

    private fun createNewReadMessage(m: Message, userId: String, status: MessageStatus) =
        createCallMessage(blazeMessageData!!.quoteMessageId!!, m.conversationId, userId, m.category, m.content,
            m.createdAt, status, m.quoteMessageId, m.mediaDuration)

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
            declineTriggeredByUser = false
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

        const val ACTION_CHECK_TIMEOUT = "check_timeout"
        const val ACTION_MUTE_AUDIO = "mute_audio"
        const val ACTION_SPEAKERPHONE = "speakerphone"

        const val EXTRA_TO_IDLE = "from_notification"
        const val EXTRA_CONVERSATION_ID = "conversation_id"
        const val EXTRA_BLAZE = "blaze"
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