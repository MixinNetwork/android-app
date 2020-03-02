package one.mixin.android.webrtc

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.gson.Gson
import dagger.android.AndroidInjection
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import one.mixin.android.Constants
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.api.service.AccountService
import one.mixin.android.crypto.Base64
import one.mixin.android.db.MessageDao
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.supportsOreo
import one.mixin.android.extension.vibrate
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendMessageJob
import one.mixin.android.repository.ConversationRepository
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
import one.mixin.android.websocket.BlazeMessageData
import org.webrtc.IceCandidate
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.StatsReport
import timber.log.Timber

class CallService : Service(), PeerConnectionClient.PeerConnectionEvents {

    private val callExecutor = Executors.newSingleThreadExecutor()
    private val timeoutExecutor = Executors.newScheduledThreadPool(1)
    private var timeoutFuture: ScheduledFuture<*>? = null

    private val audioManager: CallAudioManager by lazy {
        CallAudioManager(this)
    }
    private var audioEnable = true

    private var disposable: Disposable? = null

    private val peerConnectionClient: PeerConnectionClient by lazy {
        PeerConnectionClient(this, this)
    }

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    @field:[DatabaseCategory(DatabaseCategoryEnum.BASE)]
    lateinit var messageDao: MessageDao
    @Inject
    lateinit var accountService: AccountService
    @Inject
    lateinit var callState: one.mixin.android.vo.CallState
    @Inject
    lateinit var conversationRepo: ConversationRepository

    private val gson = Gson()

    private var blazeMessageData: BlazeMessageData? = null
    private var quoteMessageId: String? = null
    private lateinit var self: User
    private var user: User? = null
    private lateinit var conversationId: String

    private var declineTriggeredByUser: Boolean = true

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        isRunning = true
        peerConnectionClient.createPeerConnectionFactory(PeerConnectionFactory.Options())
        Session.getAccount()?.toUser().let { user ->
            if (user == null) {
                stopSelf()
            } else {
                self = user
            }
        }
        supportsOreo {
            updateNotification()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == null) {
            supportsOreo {
                updateNotification()
            }
            return START_NOT_STICKY
        }

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
                ACTION_CALL_DISCONNECT -> disconnect()

                ACTION_MUTE_AUDIO -> handleMuteAudio(intent)
                ACTION_SPEAKERPHONE -> handleSpeakerphone(intent)
                ACTION_CHECK_TIMEOUT -> handleCheckTimeout()
            }
        }
        supportsOreo {
            updateNotification()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        audioManager.release()
        callState.reset()
        isRunning = false
    }

    private fun disconnect() {
        stopForeground(true)
        audioManager.stop()
        peerConnectionClient.close()
        disposable?.dispose()
        timeoutFuture?.cancel(true)
    }

    private fun handleCallIncoming(intent: Intent) {
        if (!callState.isIdle() || isBusy()) {
            val category = MessageCategory.WEBRTC_AUDIO_BUSY.name
            val bmd = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
            val m = createCallMessage(UUID.randomUUID().toString(), bmd.conversationId, self.userId, category, null,
                nowInUtc(), MessageStatus.SENDING.name, bmd.messageId)
            jobManager.addJobInBackground(SendMessageJob(m, recipientId = bmd.userId))

            val savedMessage = createCallMessage(bmd.messageId, m.conversationId, bmd.userId, m.category, m.content,
                m.createdAt, bmd.status, bmd.messageId)
            if (checkConversation(m)) {
                messageDao.insert(savedMessage)
            }
            return
        }
        if (callState.callInfo.callState == CallState.STATE_RINGING) return

        callState.setCallState(CallState.STATE_RINGING)
        audioManager.start(false)
        blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
        user = intent.getParcelableExtra(ARGS_USER)

        val pendingCandidateData = intent.getStringExtra(EXTRA_PENDING_CANDIDATES)
        if (pendingCandidateData != null && pendingCandidateData.isNotEmpty()) {
            val list = gson.fromJson(pendingCandidateData, Array<IceCandidate>::class.java)
            list.forEach {
                peerConnectionClient.addRemoteIceCandidate(it)
            }
        }

        callState.user = user
        updateNotification()
        quoteMessageId = blazeMessageData!!.messageId
        callState.setMessageId(quoteMessageId!!)
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        peerConnectionClient.isInitiator = false
        callState.isInitiator = false
        CallActivity.show(this, user)
    }

    private fun handleCallOutgoing(intent: Intent) {
        if (callState.callInfo.callState == CallState.STATE_DIALING) return

        callState.setCallState(CallState.STATE_DIALING)
        audioManager.start(true)
        conversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        user = intent.getParcelableExtra(ARGS_USER)
        callState.user = user
        updateNotification()
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        peerConnectionClient.isInitiator = true
        callState.isInitiator = true
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
            peerConnectionClient.setAnswerSdp(getRemoteSdp(Base64.decode(blazeMessageData!!.data)))
        } else {
            getTurnServer {
                peerConnectionClient.createAnswer(it, getRemoteSdp(Base64.decode(blazeMessageData!!.data)))
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

        callState.connectedTime = System.currentTimeMillis()
        callState.setCallState(CallState.STATE_CONNECTED)
        updateNotification()
        timeoutFuture?.cancel(true)
        vibrate(longArrayOf(0, 30))
        peerConnectionClient.setAudioEnable(audioEnable)
        peerConnectionClient.enableCommunication()
    }

    private fun handleCallCancel(intent: Intent? = null) {
        if (callState.callInfo.callState == CallState.STATE_IDLE) return

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
        val state = callState.callInfo.callState
        if (state == CallState.STATE_DIALING && peerConnectionClient.hasLocalSdp()) {
            val mId = UUID.randomUUID().toString()
            val m = createCallMessage(mId, conversationId, self.userId, MessageCategory.WEBRTC_AUDIO_FAILED.name,
                null, nowInUtc(), MessageStatus.READ.name, mId)
            messageDao.insert(m)
            callState.setCallState(CallState.STATE_IDLE)
            disconnect()
        } else if (state != CallState.STATE_CONNECTED) {
            sendCallMessage(MessageCategory.WEBRTC_AUDIO_FAILED.name)
            callState.setCallState(CallState.STATE_IDLE)
            disconnect()
        }
        updateNotification()
    }

    private fun handleCallRemoteFailed() {
        callState.setCallState(CallState.STATE_IDLE)
        updateNotification()
        disconnect()
    }

    private fun handleMuteAudio(intent: Intent) {
        val extras = intent.extras ?: return

        audioEnable = !extras.getBoolean(EXTRA_MUTE)
        peerConnectionClient.setAudioEnable(audioEnable)
        updateNotification()
    }

    private fun handleSpeakerphone(intent: Intent) {
        val extras = intent.extras ?: return

        val speakerphone = extras.getBoolean(EXTRA_SPEAKERPHONE)
        audioManager.isSpeakerOn = speakerphone
        updateNotification()
    }

    private fun handleCheckTimeout() {
        if (callState.callInfo.callState == CallState.STATE_IDLE && callState.callInfo.callState == CallState.STATE_CONNECTED) return

        updateNotification()
        handleCallCancel()
    }

    private fun updateNotification() {
        startForeground(CallNotificationBuilder.WEBRTC_NOTIFICATION,
            CallNotificationBuilder.getCallNotification(this, callState, user))
    }

    private fun getRemoteSdp(json: ByteArray): SessionDescription {
        val sdp = gson.fromJson(String(json), Sdp::class.java)
        return SessionDescription(getType(sdp.type), sdp.sdp)
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
            val arr = arrayListOf(candidate)
            sendCallMessage(MessageCategory.WEBRTC_ICE_CANDIDATE.name, gson.toJson(arr))
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
    }

    override fun onIceConnected() {
        callExecutor.execute { handleIceConnected() }
    }

    override fun onIceDisconnected() {
    }

    override fun onIceConnectedFailed() {
        callExecutor.execute { handleCallLocalFailed() }
    }

    override fun onPeerConnectionClosed() {
        CallService.stopService(this)
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport>) {
    }

    override fun onPeerConnectionError(description: String) {
        callExecutor.execute { handleCallLocalFailed() }
    }

    private fun sendCallMessage(category: String, content: String? = null) {
        val message = if (peerConnectionClient.isInitiator) {
            val messageId = UUID.randomUUID().toString()
            if (category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
                quoteMessageId = messageId
                callState.setMessageId(messageId)
                createCallMessage(messageId, conversationId, self.userId, category, content, nowInUtc(), MessageStatus.SENDING.name)
            } else {
                if (category == MessageCategory.WEBRTC_AUDIO_END.name) {
                    val duration = System.currentTimeMillis() - callState.connectedTime!!
                    createCallMessage(messageId, conversationId, self.userId, category, content,
                        nowInUtc(), MessageStatus.SENDING.name, quoteMessageId, duration.toString())
                } else {
                    createCallMessage(messageId, conversationId, self.userId, category, content,
                        nowInUtc(), MessageStatus.SENDING.name, quoteMessageId)
                }
            }
        } else {
            if (blazeMessageData == null) {
                Timber.e("Answer's blazeMessageData can not be null!")
                handleCallLocalFailed()
                return
            }
            if (category == MessageCategory.WEBRTC_AUDIO_END.name) {
                val duration = System.currentTimeMillis() - callState.connectedTime!!
                createCallMessage(UUID.randomUUID().toString(), blazeMessageData!!.conversationId,
                    self.userId, category, content, nowInUtc(), MessageStatus.SENDING.name, quoteMessageId,
                    duration.toString())
            } else {
                createCallMessage(UUID.randomUUID().toString(), blazeMessageData!!.conversationId,
                    self.userId, category, content, nowInUtc(), MessageStatus.SENDING.name, quoteMessageId)
            }
        }
        val recipientId = user?.userId
        if (quoteMessageId != null || message.category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
            jobManager.addJobInBackground(SendMessageJob(message, recipientId = recipientId))
        }

        saveMessage(message)
    }

    private fun saveMessage(m: Message) {
        if (!checkConversation(m)) return

        val uId = if (callState.isInitiator) {
            self.userId
        } else {
            callState.user!!.userId
        }
        when {
            m.category == MessageCategory.WEBRTC_AUDIO_DECLINE.name -> {
                val status = if (declineTriggeredByUser) MessageStatus.READ else MessageStatus.DELIVERED
                messageDao.insert(createNewReadMessage(m, uId, status))
            }
            m.category == MessageCategory.WEBRTC_AUDIO_CANCEL.name -> {
                val msg = createCallMessage(m.id, m.conversationId, uId, m.category, m.content,
                    m.createdAt, MessageStatus.READ.name, m.quoteMessageId, m.mediaDuration)
                messageDao.insert(msg)
            }
            m.category == MessageCategory.WEBRTC_AUDIO_END.name || m.category == MessageCategory.WEBRTC_AUDIO_FAILED.name -> {
                val msg = createNewReadMessage(m, uId, MessageStatus.READ)
                messageDao.insert(msg)
            }
        }
    }

    private fun createNewReadMessage(m: Message, userId: String, status: MessageStatus) =
        createCallMessage(quoteMessageId ?: blazeMessageData?.quoteMessageId ?: blazeMessageData?.messageId
        ?: UUID.randomUUID().toString(),
            m.conversationId, userId, m.category, m.content, m.createdAt, status.name, m.quoteMessageId, m.mediaDuration)

    private fun checkConversation(message: Message): Boolean {
        val conversation = conversationRepo.getConversation(message.conversationId)
        if (conversation != null) return true

        return conversationRepo.refreshConversation(message.conversationId)
    }

    private fun getTurnServer(action: (List<PeerConnection.IceServer>) -> Unit) {
        disposable = accountService.getTurn().subscribeOn(Schedulers.io()).subscribe({
            if (it.isSuccess) {
                val array = it.data as Array<TurnServer>
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
        callExecutor.execute { handleCallLocalFailed() }
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
            CallService.timeout(context)
        }
    }

    enum class CallState {
        STATE_IDLE, STATE_DIALING, STATE_RINGING, STATE_ANSWERING, STATE_CONNECTED, STATE_BUSY
    }

    companion object {
        const val TAG = "CallService"

        const val DEFAULT_TIMEOUT_MINUTES = 1L

        private const val ACTION_CALL_INCOMING = "call_incoming"
        private const val ACTION_CALL_OUTGOING = "call_outgoing"
        const val ACTION_CALL_ANSWER = "call_answer"
        const val ACTION_CANDIDATE = "candidate"
        const val ACTION_CALL_CANCEL = "call_cancel"
        const val ACTION_CALL_DECLINE = "call_decline"
        const val ACTION_CALL_LOCAL_END = "call_local_end"
        const val ACTION_CALL_REMOTE_END = "call_remote_end"
        const val ACTION_CALL_BUSY = "call_busy"
        const val ACTION_CALL_LOCAL_FAILED = "call_local_failed"
        const val ACTION_CALL_REMOTE_FAILED = "call_remote_failed"
        const val ACTION_CALL_DISCONNECT = "call_disconnect"

        private const val ACTION_CHECK_TIMEOUT = "check_timeout"
        private const val ACTION_MUTE_AUDIO = "mute_audio"
        private const val ACTION_SPEAKERPHONE = "speakerphone"

        const val EXTRA_TO_IDLE = "from_notification"
        private const val EXTRA_CONVERSATION_ID = "conversation_id"
        private const val EXTRA_BLAZE = "blaze"
        private const val EXTRA_MUTE = "mute"
        private const val EXTRA_SPEAKERPHONE = "speakerphone"
        private const val EXTRA_PENDING_CANDIDATES = "pending_candidates"

        var isRunning = false

        fun incoming(ctx: Context, user: User, data: BlazeMessageData, pendingCandidateData: String? = null) = startService(ctx, ACTION_CALL_INCOMING) {
            it.putExtra(Constants.ARGS_USER, user)
            it.putExtra(CallService.EXTRA_BLAZE, data)
            if (pendingCandidateData != null) {
                it.putExtra(EXTRA_PENDING_CANDIDATES, pendingCandidateData)
            }
        }

        fun outgoing(ctx: Context, user: User, conversationId: String) = startService(ctx, ACTION_CALL_OUTGOING) {
            it.putExtra(Constants.ARGS_USER, user)
            it.putExtra(CallService.EXTRA_CONVERSATION_ID, conversationId)
        }

        fun answer(ctx: Context, data: BlazeMessageData? = null) = startService(ctx, CallService.ACTION_CALL_ANSWER) { intent ->
            data?.let {
                intent.putExtra(CallService.EXTRA_BLAZE, data)
            }
        }

        fun candidate(ctx: Context, data: BlazeMessageData) = startService(ctx, CallService.ACTION_CANDIDATE) {
            it.putExtra(CallService.EXTRA_BLAZE, data)
        }

        fun cancel(ctx: Context) = startService(ctx, CallService.ACTION_CALL_CANCEL)

        fun decline(ctx: Context) = startService(ctx, CallService.ACTION_CALL_DECLINE)

        fun localEnd(ctx: Context) = startService(ctx, CallService.ACTION_CALL_LOCAL_END)

        fun remoteEnd(ctx: Context) = startService(ctx, CallService.ACTION_CALL_REMOTE_END)

        fun busy(ctx: Context) = startService(ctx, CallService.ACTION_CALL_BUSY)

        fun remoteFailed(ctx: Context) = startService(ctx, CallService.ACTION_CALL_REMOTE_FAILED)

        fun disconnect(ctx: Context) {
            if (isRunning) {
                startService(ctx, ACTION_CALL_DISCONNECT)
            }
        }

        fun muteAudio(ctx: Context, checked: Boolean) = startService(ctx, CallService.ACTION_MUTE_AUDIO) {
            it.putExtra(CallService.EXTRA_MUTE, checked)
        }

        fun speakerPhone(ctx: Context, checked: Boolean) = startService(ctx, CallService.ACTION_SPEAKERPHONE) {
            it.putExtra(CallService.EXTRA_SPEAKERPHONE, checked)
        }

        fun timeout(ctx: Context) = startService(ctx, ACTION_CHECK_TIMEOUT)

        private fun startService(ctx: Context, action: String? = null, putExtra: ((intent: Intent) -> Unit)? = null) {
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
