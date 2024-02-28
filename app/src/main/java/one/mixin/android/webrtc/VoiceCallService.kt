package one.mixin.android.webrtc

import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.crypto.Base64
import one.mixin.android.db.insertAndNotifyConversation
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.getParcelableExtraCompat
import one.mixin.android.extension.getSerializableExtraCompat
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.SendMessageJob
import one.mixin.android.ui.call.CallActivity
import one.mixin.android.vo.CallType
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Sdp
import one.mixin.android.vo.User
import one.mixin.android.vo.createCallMessage
import one.mixin.android.vo.getSdp
import one.mixin.android.websocket.BlazeMessageData
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class VoiceCallService : CallService() {
    private var blazeMessageData: BlazeMessageData? = null
    private var declineTriggeredByUser: Boolean = true

    override fun handleIntent(intent: Intent): Boolean {
        initWebRtc()
        var handled = true
        when (intent.action) {
            ACTION_CALL_INCOMING -> handleCallIncoming(intent)
            ACTION_CALL_OUTGOING -> handleCallOutgoing(intent)
            ACTION_CALL_ANSWER -> handleAnswerCall(intent)
            ACTION_CANDIDATE -> handleCandidate(intent)
            ACTION_CALL_CANCEL -> handleCallCancel()
            ACTION_CALL_DECLINE -> handleCallDecline()
            ACTION_CALL_LOCAL_END -> handleLocalEnd()
            ACTION_CALL_REMOTE_END -> handleCallRemoteEnd()
            ACTION_CALL_BUSY -> handleCallBusy()
            ACTION_CALL_LOCAL_FAILED -> handleCallLocalFailed()
            ACTION_CALL_REMOTE_FAILED -> handleCallRemoteFailed()
            else -> handled = false
        }
        return handled
    }

    private fun handleCallIncoming(intent: Intent) {
        val blazeMessageData = intent.getSerializableExtraCompat(EXTRA_BLAZE, BlazeMessageData::class.java) ?: return
        val sdp = getSdp(blazeMessageData.data.decodeBase64()) ?: return
        val user = intent.getParcelableExtraCompat(ARGS_USER, User::class.java)

        if (user?.userId == callState.user?.userId) {
            peerConnectionClient.createAnswer(
                null,
                sdp,
                setLocalSuccess = {
                    sendCallMessage(MessageCategory.WEBRTC_AUDIO_ANSWER.name, gson.toJson(Sdp(it.description, it.type.canonicalForm())))
                },
            )
            return
        }

        if (callState.isBusy()) {
            val category = MessageCategory.WEBRTC_AUDIO_BUSY.name
            val bmd = intent.getSerializableExtraCompat(EXTRA_BLAZE, BlazeMessageData::class.java) ?: return
            val m =
                createCallMessage(
                    UUID.randomUUID().toString(),
                    bmd.conversationId,
                    self.userId,
                    category,
                    null,
                    nowInUtc(),
                    MessageStatus.SENDING.name,
                    bmd.messageId,
                )
            jobManager.addJobInBackground(SendMessageJob(m, recipientId = bmd.userId))

            val savedMessage =
                createCallMessage(
                    bmd.messageId,
                    m.conversationId,
                    bmd.userId,
                    m.category,
                    m.content,
                    m.createdAt,
                    bmd.status,
                    bmd.messageId,
                )
            if (checkConversation(m)) {
                insertCallMessage(savedMessage)
            }
            return
        }
        if (callState.state == CallState.STATE_RINGING) return

        if (isDisconnected.compareAndSet(true, false)) {
            callState.state = CallState.STATE_RINGING
            callState.callType = CallType.Voice

            val pendingCandidateData = intent.getStringExtra(EXTRA_PENDING_CANDIDATES)
            if (pendingCandidateData != null && pendingCandidateData.isNotEmpty()) {
                val list = gson.fromJson(pendingCandidateData, Array<IceCandidate>::class.java)
                list.forEach {
                    peerConnectionClient.addRemoteIceCandidate(it)
                }
            }
            this.blazeMessageData = blazeMessageData
            callState.user = user
            callState.trackId = blazeMessageData.messageId
            updateForegroundNotification()
            timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            callState.isOffer = false
            CallActivity.show(this)
            audioManager.start(false)
        }
    }

    private fun handleCallOutgoing(intent: Intent) {
        if (callState.state == CallState.STATE_DIALING) return

        if (isDisconnected.compareAndSet(true, false)) {
            callState.state = CallState.STATE_DIALING
            callState.callType = CallType.Voice
            val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
            require(cid != null)
            callState.conversationId = cid
            val user = intent.getParcelableExtraCompat(ARGS_USER, User::class.java)
            callState.user = user
            updateForegroundNotification()
            callState.isOffer = true
            timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
            CallActivity.show(this)
            audioManager.start(true)
            getTurnServer { turns ->
                peerConnectionClient.createOffer(
                    turns,
                    setLocalSuccess = {
                        sendCallMessage(MessageCategory.WEBRTC_AUDIO_OFFER.name, gson.toJson(Sdp(it.description, it.type.canonicalForm())))
                    },
                )
            }
        }
    }

    private fun handleAnswerCall(intent: Intent) {
        if (callState.state == CallState.STATE_ANSWERING ||
            callState.isIdle()
        ) {
            return
        }

        if (!callState.isConnected()) {
            callState.state = CallState.STATE_ANSWERING
            callState.callType = CallType.Voice
            updateForegroundNotification()
            audioManager.stop()
        }
        if (callState.isOffer) {
            val bmd = intent.getSerializableExtraCompat(EXTRA_BLAZE, BlazeMessageData::class.java) ?: return
            this.blazeMessageData = bmd
            peerConnectionClient.setAnswerSdp(getRemoteSdp(Base64.decode(bmd.data)))
        } else {
            getTurnServer { turns ->
                val bmd = this.blazeMessageData
                if (bmd == null) {
                    Timber.e("$TAG_CALL try answer a call, but blazeMessageData is null")
                    return@getTurnServer
                }
                val sdp = getSdp(bmd.data.decodeBase64()) ?: return@getTurnServer
                peerConnectionClient.createAnswer(
                    turns,
                    sdp,
                    setLocalSuccess = {
                        sendCallMessage(MessageCategory.WEBRTC_AUDIO_ANSWER.name, gson.toJson(Sdp(it.description, it.type.canonicalForm())))
                    },
                )
            }
        }
    }

    private fun handleCandidate(intent: Intent) {
        val blazeMessageData = intent.getSerializableExtraCompat(EXTRA_BLAZE, BlazeMessageData::class.java) ?: return
        val json = String(Base64.decode(blazeMessageData.data))
        val ices = gson.fromJson(json, Array<IceCandidate>::class.java)
        ices.forEach {
            peerConnectionClient.addRemoteIceCandidate(it)
        }
    }

    private fun handleCallCancel() {
        if (callState.isIdle()) return

        if (callState.isOffer) {
            val category = MessageCategory.WEBRTC_AUDIO_CANCEL.name
            sendCallMessage(category)
        }
        audioManager.stop()
        disconnect()
    }

    private fun handleCallDecline() {
        if (callState.isIdle()) return

        if (!callState.isOffer) {
            val category = MessageCategory.WEBRTC_AUDIO_DECLINE.name
            sendCallMessage(category)
        }
        audioManager.stop()
        disconnect()
    }

    override fun needInitWebRtc(action: String) = true

    override fun handleLocalEnd() {
        if (callState.isIdle()) return

        val category = MessageCategory.WEBRTC_AUDIO_END.name
        sendCallMessage(category)
        disconnect()
    }

    override fun onTimeout() {
        callExecutor.execute {
            handleCallCancel()
        }
    }

    override fun onTurnServerError() {
        callExecutor.execute {
            handleCallLocalFailed()
        }
    }

    override fun onCallDisconnected() {
        // Left empty
    }

    override fun onDestroyed() {
        // Left empty
    }

    private fun handleCallRemoteEnd() {
        if (callState.isIdle()) return

        disconnect()
    }

    private fun handleCallBusy() {
        if (callState.isIdle()) return

        disconnect()
    }

    private fun handleCallLocalFailed() {
        if (callState.isIdle()) return

        val state = callState.state
        if (state == CallState.STATE_DIALING && peerConnectionClient.hasLocalSdp()) {
            val mId = UUID.randomUUID().toString()
            val cid = callState.conversationId
            if (cid == null) {
                Timber.e("$TAG_CALL try save WEBRTC_AUDIO_FAILED message, but conversation id is null")
                return
            }
            val m =
                createCallMessage(
                    mId,
                    cid,
                    self.userId,
                    MessageCategory.WEBRTC_AUDIO_FAILED.name,
                    null,
                    nowInUtc(),
                    MessageStatus.READ.name,
                    mId,
                )
            insertCallMessage(m)
        } else if (state != CallState.STATE_CONNECTED) {
            sendCallMessage(MessageCategory.WEBRTC_AUDIO_FAILED.name)
        }
        disconnect()
    }

    private fun handleCallRemoteFailed() {
        if (callState.isIdle()) return

        disconnect()
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

    override fun onIceCandidate(candidate: IceCandidate) {
        callExecutor.execute {
            if (!callState.isVoiceCall()) {
                return@execute
            }
            val arr = arrayListOf(candidate)
            sendCallMessage(MessageCategory.WEBRTC_ICE_CANDIDATE.name, gson.toJson(arr))
        }
    }

    override fun onPeerConnectionError(errorMsg: String) {
        super.onPeerConnectionError(errorMsg)
        callExecutor.execute { handleCallLocalFailed() }
    }

    override fun onIceFailed() {
        if (!callState.isConnected() || callState.reconnecting) return
        if (!callState.isOffer) return

        callExecutor.execute {
            callState.reconnecting = true
            peerConnectionClient.createOffer(
                null,
                setLocalSuccess = {
                    sendCallMessage(MessageCategory.WEBRTC_AUDIO_OFFER.name, gson.toJson(Sdp(it.description, it.type.canonicalForm())))
                },
            )
        }
    }

    override fun getSenderPublicKey(
        userId: String,
        sessionId: String,
    ): ByteArray? {
        return null
    }

    override fun requestResendKey(
        userId: String,
        sessionId: String,
    ) {}

    private fun sendCallMessage(
        category: String,
        content: String? = null,
    ) {
        val quoteMessageId = callState.trackId
        val message =
            if (callState.isOffer) {
                val messageId = UUID.randomUUID().toString()
                val conversationId = callState.conversationId
                if (conversationId == null) {
                    Timber.e("$TAG_CALL try send call message but conversation id is null")
                    return
                }
                if (category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
                    if (callState.trackId == null) {
                        callState.trackId = messageId
                    }
                    createCallMessage(messageId, conversationId, self.userId, category, content, nowInUtc(), MessageStatus.SENDING.name, quoteMessageId)
                } else {
                    if (category == MessageCategory.WEBRTC_AUDIO_END.name) {
                        var connectedTime = callState.connectedTime
                        if (connectedTime == null) {
                            Timber.e("$TAG_CALL try create WEBRTC_AUDIO_END message, but connected time is null")
                            connectedTime = System.currentTimeMillis()
                        }
                        val duration = System.currentTimeMillis() - connectedTime
                        createCallMessage(
                            messageId, conversationId, self.userId, category, content,
                            nowInUtc(), MessageStatus.SENDING.name, quoteMessageId, duration.toString(),
                        )
                    } else {
                        createCallMessage(
                            messageId,
                            conversationId,
                            self.userId,
                            category,
                            content,
                            nowInUtc(),
                            MessageStatus.SENDING.name,
                            quoteMessageId,
                        )
                    }
                }
            } else {
                val blazeMessageData = this.blazeMessageData
                if (blazeMessageData == null) {
                    Timber.e("$TAG_CALL Answer's blazeMessageData can not be null!")
                    handleCallLocalFailed()
                    return
                }
                if (category == MessageCategory.WEBRTC_AUDIO_END.name) {
                    var connectedTime = callState.connectedTime
                    if (connectedTime == null) {
                        Timber.e("$TAG_CALL try create WEBRTC_AUDIO_END message, but connected time is null")
                        connectedTime = System.currentTimeMillis()
                    }
                    val duration = System.currentTimeMillis() - connectedTime
                    createCallMessage(
                        UUID.randomUUID().toString(), blazeMessageData.conversationId,
                        self.userId, category, content, nowInUtc(), MessageStatus.SENDING.name, quoteMessageId,
                        duration.toString(),
                    )
                } else {
                    createCallMessage(
                        UUID.randomUUID().toString(),
                        blazeMessageData.conversationId,
                        self.userId,
                        category,
                        content,
                        nowInUtc(),
                        MessageStatus.SENDING.name,
                        quoteMessageId,
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

        val uId =
            if (callState.isOffer) {
                self.userId
            } else {
                val uid = callState.user?.userId
                if (uid == null) {
                    Timber.e("$TAG_CALL try save a non-offer message, but userId is null")
                    return
                }
                uid
            }
        when (m.category) {
            MessageCategory.WEBRTC_AUDIO_DECLINE.name -> {
                val status = if (declineTriggeredByUser) MessageStatus.READ else MessageStatus.DELIVERED
                insertCallMessage(createNewReadMessage(m, uId, status))
            }
            MessageCategory.WEBRTC_AUDIO_CANCEL.name -> {
                val msg =
                    createCallMessage(
                        m.messageId, m.conversationId, uId, m.category, m.content,
                        m.createdAt, MessageStatus.READ.name, m.quoteMessageId, m.mediaDuration,
                    )
                insertCallMessage(msg)
            }
            MessageCategory.WEBRTC_AUDIO_END.name, MessageCategory.WEBRTC_AUDIO_FAILED.name -> {
                val msg = createNewReadMessage(m, uId, MessageStatus.READ)
                insertCallMessage(msg)
            }
        }
    }

    private fun insertCallMessage(message: Message) {
        database.insertAndNotifyConversation(message)
        database.conversationDao().findConversationById(message.conversationId)?.let {
            val expiredIn = it.expireIn ?: return@let
            if (it.expireIn > 0) {
                database.expiredMessageDao().insert(ExpiredMessage(message.messageId, expiredIn, null))
            }
        }
    }

    private fun createNewReadMessage(
        m: Message,
        userId: String,
        status: MessageStatus,
    ): Message {
        var mId = callState.trackId ?: blazeMessageData?.quoteMessageId ?: blazeMessageData?.messageId
        if (mId.isNullOrBlank()) {
            mId = UUID.randomUUID().toString()
        }
        return createCallMessage(mId, m.conversationId, userId, m.category, m.content, m.createdAt, status.name, m.quoteMessageId, m.mediaDuration)
    }

    companion object {
        const val TAG = "VoiceCallService"
    }
}

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

fun incomingCall(
    ctx: Context,
    user: User,
    data: BlazeMessageData,
    pendingCandidateData: String? = null,
) {
    startService<VoiceCallService>(ctx, ACTION_CALL_INCOMING) {
        it.putExtra(ARGS_USER, user)
        it.putExtra(EXTRA_BLAZE, data)
        if (pendingCandidateData != null) {
            it.putExtra(EXTRA_PENDING_CANDIDATES, pendingCandidateData)
        }
    }
}

fun outgoingCall(
    ctx: Context,
    conversationId: String,
    user: User? = null,
) =
    startService<VoiceCallService>(ctx, ACTION_CALL_OUTGOING) {
        it.putExtra(ARGS_USER, user)
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }

fun answerCall(
    ctx: Context,
    data: BlazeMessageData? = null,
) =
    startService<VoiceCallService>(ctx, ACTION_CALL_ANSWER) { intent ->
        data?.let {
            intent.putExtra(EXTRA_BLAZE, data)
        }
    }

fun candidate(
    ctx: Context,
    data: BlazeMessageData,
) =
    startService<VoiceCallService>(ctx, ACTION_CANDIDATE) {
        it.putExtra(EXTRA_BLAZE, data)
    }

fun cancelCall(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_CANCEL) {}

fun declineCall(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_DECLINE) {}

fun localEnd(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_LOCAL_END) {}

fun remoteEnd(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_REMOTE_END) {}

fun busy(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_BUSY) {}

fun remoteFailed(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_REMOTE_FAILED) {}
