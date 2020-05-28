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
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.api.service.AccountService
import one.mixin.android.crypto.Base64
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.insertAndNotifyConversation
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
import one.mixin.android.vo.CallStateLiveData
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
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

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
    lateinit var database: MixinDatabase
    @Inject
    lateinit var accountService: AccountService
    @Inject
    lateinit var callState: CallStateLiveData
    @Inject
    lateinit var conversationRepo: ConversationRepository

    private val gson = Gson()

    private var blazeMessageData: BlazeMessageData? = null
    private lateinit var self: User

    private var declineTriggeredByUser = true

    private var isDestroyed = AtomicBoolean(false)

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
        peerConnectionClient.createPeerConnectionFactory(PeerConnectionFactory.Options())
        Session.getAccount()?.toUser().let { user ->
            if (user == null) {
                stopSelf()
            } else {
                self = user
            }
        }
        supportsOreo {
            updateForegroundNotification()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        supportsOreo {
            updateForegroundNotification()
        }

        if (intent == null || intent.action == null || isDestroyed.get()) {
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
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        if (isDestroyed.compareAndSet(false, true)) {
            audioManager.release()
            callState.reset()
        }
    }

    private fun disconnect() {
        if (isDestroyed.get()) return

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
            val m = createCallMessage(
                UUID.randomUUID().toString(), bmd.conversationId, self.userId, category, null,
                nowInUtc(), MessageStatus.SENDING.name, bmd.messageId
            )
            jobManager.addJobInBackground(SendMessageJob(m, recipientId = bmd.userId))

            val savedMessage = createCallMessage(
                bmd.messageId, m.conversationId, bmd.userId, m.category, m.content,
                m.createdAt, bmd.status, bmd.messageId
            )
            if (checkConversation(m)) {
                database.insertAndNotifyConversation(savedMessage)
            }
            return
        }
        if (callState.state == CallState.STATE_RINGING) return

        callState.state = CallState.STATE_RINGING
        blazeMessageData = intent.getSerializableExtra(EXTRA_BLAZE) as BlazeMessageData
        val user = intent.getParcelableExtra<User>(ARGS_USER)
        val users = intent.getParcelableArrayListExtra<User>(EXTRA_USERS)

        val pendingCandidateData = intent.getStringExtra(EXTRA_PENDING_CANDIDATES)
        if (pendingCandidateData != null && pendingCandidateData.isNotEmpty()) {
            val list = gson.fromJson(pendingCandidateData, Array<IceCandidate>::class.java)
            list.forEach {
                peerConnectionClient.addRemoteIceCandidate(it)
            }
        }

        callState.user = user
        callState.users = users
        callState.isOffer = false
        callState.trackId = blazeMessageData!!.messageId
        updateForegroundNotification()
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        CallActivity.show(this)
        audioManager.start(false)
    }

    private fun handleCallOutgoing(intent: Intent) {
        if (callState.state == CallState.STATE_DIALING) return

        callState.state = CallState.STATE_DIALING
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        require(cid != null)
        val user = intent.getParcelableExtra<User>(ARGS_USER)
        val users = intent.getParcelableArrayListExtra<User>(EXTRA_USERS)
        callState.user = user
        callState.users = users
        callState.conversationId = cid
        callState.isOffer = true
        updateForegroundNotification()
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(this), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        CallActivity.show(this)
        audioManager.start(true)
        getTurnServer { peerConnectionClient.createOffer(it) }
    }

    private fun handleAnswerCall(intent: Intent) {
        if (callState.state == CallState.STATE_ANSWERING ||
            callState.isIdle()
        ) return

        callState.state = CallState.STATE_ANSWERING
        updateForegroundNotification()
        audioManager.stop()

        if (callState.isOffer) {
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
    }

    private fun handleConnected() {
        if (callState.isConnected()) return

        callState.connectedTime = System.currentTimeMillis()
        callState.state = CallState.STATE_CONNECTED
        updateForegroundNotification()
        timeoutFuture?.cancel(true)
        vibrate(longArrayOf(0, 30))
        peerConnectionClient.setAudioEnable(audioEnable)
        peerConnectionClient.enableCommunication()
    }

    private fun handleCallCancel(intent: Intent? = null) {
        if (callState.isIdle()) return

        audioManager.stop()
        if (callState.isOffer) {
            val category = MessageCategory.WEBRTC_AUDIO_CANCEL.name
            sendVoiceCallMessage(category)
            val toIdle = intent?.getBooleanExtra(EXTRA_TO_IDLE, false)
            if (toIdle != null && toIdle) {
                callState.state = CallState.STATE_IDLE
            }
        } else {
            callState.state = CallState.STATE_IDLE
        }
        disconnect()
    }

    private fun handleCallDecline() {
        if (callState.isIdle()) return

        audioManager.stop()
        callState.state = CallState.STATE_IDLE
        if (!callState.isOffer) {
            val category = MessageCategory.WEBRTC_AUDIO_DECLINE.name
            sendVoiceCallMessage(category)
        }
        disconnect()
    }

    private fun handleCallLocalEnd(intent: Intent? = null) {
        if (callState.isIdle()) return

        val category = MessageCategory.WEBRTC_AUDIO_END.name
        sendVoiceCallMessage(category)
        val toIdle = intent?.getBooleanExtra(EXTRA_TO_IDLE, false)
        if (toIdle != null && toIdle) {
            callState.state = CallState.STATE_IDLE
        }
        disconnect()
    }

    private fun handleCallRemoteEnd() {
        if (callState.isIdle()) return

        callState.state = CallState.STATE_IDLE
        disconnect()
    }

    private fun handleCallBusy() {
        callState.state = CallState.STATE_BUSY
        disconnect()
    }

    private fun handleCallLocalFailed() {
        if (callState.isIdle()) return

        val state = callState.state
        callState.state = CallState.STATE_IDLE
        if (state == CallState.STATE_DIALING && peerConnectionClient.hasLocalSdp()) {
            val mId = UUID.randomUUID().toString()
            val m = createCallMessage(
                mId, callState.conversationId!!, self.userId, MessageCategory.WEBRTC_AUDIO_FAILED.name,
                null, nowInUtc(), MessageStatus.READ.name, mId
            )
            database.insertAndNotifyConversation(m)
            disconnect()
        } else if (state != CallState.STATE_CONNECTED) {
            sendVoiceCallMessage(MessageCategory.WEBRTC_AUDIO_FAILED.name)
            disconnect()
        }
    }

    private fun handleCallRemoteFailed() {
        if (callState.isIdle()) return

        callState.state = CallState.STATE_IDLE
        disconnect()
    }

    private fun handleMuteAudio(intent: Intent) {
        val extras = intent.extras ?: return

        audioEnable = !extras.getBoolean(EXTRA_MUTE)
        peerConnectionClient.setAudioEnable(audioEnable)
    }

    private fun handleSpeakerphone(intent: Intent) {
        val extras = intent.extras ?: return

        val speakerphone = extras.getBoolean(EXTRA_SPEAKERPHONE)
        audioManager.isSpeakerOn = speakerphone
    }

    private fun handleCheckTimeout() {
        if (callState.isIdle() || callState.isConnected()) return

        handleCallCancel()
    }

    private fun updateForegroundNotification() {
        CallNotificationBuilder.getCallNotification(this, callState)?.let {
            startForeground(CallNotificationBuilder.WEBRTC_NOTIFICATION, it)
        }
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
        return callState.state != CallState.STATE_IDLE || tm?.callState != TelephonyManager.CALL_STATE_IDLE
    }

    private fun getCategory(): String {
        return if (callState.isOffer) {
            if (callState.isGroupCall()) {
                MessageCategory.KRAKEN_PUBLISH.name
            } else {
                MessageCategory.WEBRTC_AUDIO_OFFER.name
            }
        } else {
            if (callState.isGroupCall()) {
                MessageCategory.KRAKEN_ANSWER.name
            } else {
                MessageCategory.WEBRTC_AUDIO_ANSWER.name
            }
        }
    }

    // PeerConnectionEvents
    override fun onLocalDescription(sdp: SessionDescription) {
        callExecutor.execute {
            val category = getCategory()
            if (callState.isGroupCall()) {
                sendGroupCallMessage(category, gson.toJson(Sdp(sdp.description, sdp.type.canonicalForm())))
            } else {
                sendVoiceCallMessage(category, gson.toJson(Sdp(sdp.description, sdp.type.canonicalForm())))
            }
        }
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        callExecutor.execute {
            val arr = arrayListOf(candidate)
            sendVoiceCallMessage(MessageCategory.WEBRTC_ICE_CANDIDATE.name, gson.toJson(arr))
        }
    }

    override fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {
    }

    override fun onIceConnected() {
    }

    override fun onConnected() {
        callExecutor.execute { handleConnected() }
    }

    override fun onDisconnected() {
        callExecutor.execute {
            handleCallLocalEnd()
        }
    }

    override fun onIceDisconnected() {
    }

    override fun onPeerConnectionClosed() {
        stopService(this)
    }

    override fun onPeerConnectionStatsReady(reports: Array<StatsReport>) {
    }

    override fun onPeerConnectionError(description: String) {
        callExecutor.execute { handleCallLocalFailed() }
    }

    private fun sendGroupCallMessage(category: String, content: String? = null) {
        val message = createCallMessage(
            UUID.randomUUID().toString(), callState.conversationId!!,
            self.userId, category, content, nowInUtc(), MessageStatus.SENDING.name
        )
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    private fun sendVoiceCallMessage(category: String, content: String? = null) {
        val quoteMessageId = callState.trackId
        val message = if (callState.isOffer) {
            val messageId = UUID.randomUUID().toString()
            val conversationId = callState.conversationId!!
            if (category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
                callState.trackId = messageId
                createCallMessage(
                    messageId, conversationId, self.userId, category, content,
                    nowInUtc(), MessageStatus.SENDING.name
                )
            } else {
                if (category == MessageCategory.WEBRTC_AUDIO_END.name) {
                    val duration = System.currentTimeMillis() - callState.connectedTime!!
                    createCallMessage(
                        messageId, conversationId, self.userId, category, content,
                        nowInUtc(), MessageStatus.SENDING.name, quoteMessageId, duration.toString()
                    )
                } else {
                    createCallMessage(
                        messageId, conversationId, self.userId, category, content,
                        nowInUtc(), MessageStatus.SENDING.name, quoteMessageId
                    )
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
                createCallMessage(
                    UUID.randomUUID().toString(), blazeMessageData!!.conversationId,
                    self.userId, category, content, nowInUtc(), MessageStatus.SENDING.name, quoteMessageId,
                    duration.toString()
                )
            } else {
                createCallMessage(
                    UUID.randomUUID().toString(), blazeMessageData!!.conversationId,
                    self.userId, category, content, nowInUtc(), MessageStatus.SENDING.name, quoteMessageId
                )
            }
        }
        val recipientId = callState.user?.userId
        if (quoteMessageId != null || message.category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
            jobManager.addJobInBackground(SendMessageJob(message, recipientId = recipientId))
        }
        saveMessage(message)
    }

    private fun saveMessage(m: Message) {
        if (!checkConversation(m)) return

        val uId = if (callState.isOffer) {
            self.userId
        } else {
            callState.user!!.userId
        }
        when (m.category) {
            MessageCategory.WEBRTC_AUDIO_DECLINE.name -> {
                val status = if (declineTriggeredByUser) MessageStatus.READ else MessageStatus.DELIVERED
                database.insertAndNotifyConversation(createNewReadMessage(m, uId, status))
            }
            MessageCategory.WEBRTC_AUDIO_CANCEL.name -> {
                val msg = createCallMessage(
                    m.id, m.conversationId, uId, m.category, m.content,
                    m.createdAt, MessageStatus.READ.name, m.quoteMessageId, m.mediaDuration
                )
                database.insertAndNotifyConversation(msg)
            }
            MessageCategory.WEBRTC_AUDIO_END.name, MessageCategory.WEBRTC_AUDIO_FAILED.name -> {
                val msg = createNewReadMessage(m, uId, MessageStatus.READ)
                database.insertAndNotifyConversation(msg)
            }
        }
    }

    private fun createNewReadMessage(m: Message, userId: String, status: MessageStatus) =
        createCallMessage(
            callState.trackId ?: blazeMessageData?.quoteMessageId ?: blazeMessageData?.messageId
                ?: UUID.randomUUID().toString(),
            m.conversationId, userId, m.category, m.content, m.createdAt, status.name, m.quoteMessageId, m.mediaDuration
        )

    private fun checkConversation(message: Message): Boolean {
        val conversation = conversationRepo.getConversation(message.conversationId)
        if (conversation != null) return true

        return conversationRepo.refreshConversation(message.conversationId)
    }

    private fun getTurnServer(action: (List<PeerConnection.IceServer>) -> Unit) {
        disposable = accountService.getTurn().subscribeOn(Schedulers.io()).subscribe(
            {
                if (it.isSuccess) {
                    val array = it.data as Array<TurnServer>
                    action.invoke(genIceServerList(array))
                } else {
                    handleFetchTurnError()
                }
            },
            {
                ErrorHandler.handleError(it)
                handleFetchTurnError()
            }
        )
    }

    private fun handleFetchTurnError() {
        callExecutor.execute { handleCallLocalFailed() }
    }

    private fun genIceServerList(array: Array<TurnServer>): List<PeerConnection.IceServer> {
        val iceServer = arrayListOf<PeerConnection.IceServer>()
        array.forEach {
            iceServer.add(
                PeerConnection.IceServer.builder(it.url)
                    .setUsername(it.username)
                    .setPassword(it.credential)
                    .createIceServer()
            )
        }
        return iceServer
    }

    private class TimeoutRunnable(private val context: Context) : Runnable {
        override fun run() {
            timeout(context)
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
        private const val EXTRA_USERS = "users"
        private const val EXTRA_BLAZE = "blaze"
        private const val EXTRA_MUTE = "mute"
        private const val EXTRA_SPEAKERPHONE = "speakerphone"
        private const val EXTRA_PENDING_CANDIDATES = "pending_candidates"

        fun incoming(ctx: Context, user: User, data: BlazeMessageData, pendingCandidateData: String? = null) = startService(ctx, ACTION_CALL_INCOMING) {
            it.putExtra(ARGS_USER, user)
            it.putExtra(EXTRA_BLAZE, data)
            if (pendingCandidateData != null) {
                it.putExtra(EXTRA_PENDING_CANDIDATES, pendingCandidateData)
            }
        }

        fun outgoing(ctx: Context, conversationId: String, user: User? = null, users: ArrayList<User>? = null) = startService(ctx, ACTION_CALL_OUTGOING) {
            it.putExtra(ARGS_USER, user)
            it.putExtra(EXTRA_USERS, users)
            it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
        }

        fun answer(ctx: Context, data: BlazeMessageData? = null) = startService(ctx, ACTION_CALL_ANSWER) { intent ->
            data?.let {
                intent.putExtra(EXTRA_BLAZE, data)
            }
        }

        fun candidate(ctx: Context, data: BlazeMessageData) = startService(ctx, ACTION_CANDIDATE) {
            it.putExtra(EXTRA_BLAZE, data)
        }

        fun cancel(ctx: Context) = startService(ctx, ACTION_CALL_CANCEL)

        fun decline(ctx: Context) = startService(ctx, ACTION_CALL_DECLINE)

        fun localEnd(ctx: Context) = startService(ctx, ACTION_CALL_LOCAL_END)

        fun remoteEnd(ctx: Context) = startService(ctx, ACTION_CALL_REMOTE_END)

        fun busy(ctx: Context) = startService(ctx, ACTION_CALL_BUSY)

        fun remoteFailed(ctx: Context) = startService(ctx, ACTION_CALL_REMOTE_FAILED)

        fun disconnect(ctx: Context) {
            val intent = Intent(ctx, CallService::class.java).apply {
                action = ACTION_CALL_DISCONNECT
            }
            ctx.startService(intent)
        }

        fun muteAudio(ctx: Context, checked: Boolean) = startService(ctx, ACTION_MUTE_AUDIO) {
            it.putExtra(EXTRA_MUTE, checked)
        }

        fun speakerPhone(ctx: Context, checked: Boolean) = startService(ctx, ACTION_SPEAKERPHONE) {
            it.putExtra(EXTRA_SPEAKERPHONE, checked)
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
