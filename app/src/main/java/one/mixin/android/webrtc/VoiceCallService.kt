package one.mixin.android.webrtc

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.core.content.getSystemService
import one.mixin.android.Constants.ARGS_USER
import one.mixin.android.crypto.Base64
import one.mixin.android.db.insertAndNotifyConversation
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.SendMessageJob
import one.mixin.android.ui.call.CallActivity
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

class VoiceCallService : CallService() {

    private var blazeMessageData: BlazeMessageData? = null
    private var declineTriggeredByUser: Boolean = true

    override fun handleIntent(intent: Intent): Boolean {
        var handled = true
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
            else -> handled = false
        }
        return handled
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

        val pendingCandidateData = intent.getStringExtra(EXTRA_PENDING_CANDIDATES)
        if (pendingCandidateData != null && pendingCandidateData.isNotEmpty()) {
            val list = gson.fromJson(pendingCandidateData, Array<IceCandidate>::class.java)
            list.forEach {
                peerConnectionClient.addRemoteIceCandidate(it)
            }
        }

        callState.user = user
        updateForegroundNotification()
        callState.trackId = blazeMessageData!!.messageId
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        callState.isOffer = false
        CallActivity.show(this)
        audioManager.start(false)
    }

    private fun handleCallOutgoing(intent: Intent) {
        if (callState.state == CallState.STATE_DIALING) return

        callState.state = CallState.STATE_DIALING
        val cid = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        require(cid != null)
        callState.conversationId = cid
        val user = intent.getParcelableExtra<User>(ARGS_USER)
        callState.user = user
        callState.isOffer = true
        updateForegroundNotification()
        timeoutFuture = timeoutExecutor.schedule(TimeoutRunnable(), DEFAULT_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        CallActivity.show(this)
        audioManager.start(true)
        getTurnServer { turns ->
            peerConnectionClient.createOffer(
                turns,
                setLocalSuccess = {
                    sendCallMessage(MessageCategory.WEBRTC_AUDIO_OFFER.name, gson.toJson(Sdp(it.description, it.type.canonicalForm())))
                }
            )
        }
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
            getTurnServer { turns ->
                peerConnectionClient.createAnswerWithIceServer(
                    turns, getSdp(blazeMessageData!!.data.decodeBase64()),
                    setLocalSuccess = {
                        sendCallMessage(MessageCategory.WEBRTC_AUDIO_ANSWER.name, gson.toJson(Sdp(it.description, it.type.canonicalForm())))
                    }
                )
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

    override fun handleCallCancel(intent: Intent?) {
        if (callState.isIdle()) return

        if (callState.isOffer) {
            val category = MessageCategory.WEBRTC_AUDIO_CANCEL.name
            sendCallMessage(category)
        }
        disconnect()
    }

    private fun handleCallDecline() {
        if (callState.isIdle()) return

        if (!callState.isOffer) {
            val category = MessageCategory.WEBRTC_AUDIO_DECLINE.name
            sendCallMessage(category)
        }
        disconnect()
    }

    override fun handleCallLocalEnd(intent: Intent?) {
        if (callState.isIdle()) return

        val category = MessageCategory.WEBRTC_AUDIO_END.name
        sendCallMessage(category)
        disconnect()
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

    override fun handleCallLocalFailed() {
        if (callState.isIdle()) return

        val state = callState.state
        if (state == CallState.STATE_DIALING && peerConnectionClient.hasLocalSdp()) {
            val mId = UUID.randomUUID().toString()
            val m = createCallMessage(
                mId, callState.conversationId!!, self.userId, MessageCategory.WEBRTC_AUDIO_FAILED.name,
                null, nowInUtc(), MessageStatus.READ.name, mId
            )
            database.insertAndNotifyConversation(m)
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

    private fun isBusy(): Boolean {
        val tm = getSystemService<TelephonyManager>()
        return callState.state != CallState.STATE_IDLE || tm?.callState != TelephonyManager.CALL_STATE_IDLE
    }

    override fun onIceCandidate(candidate: IceCandidate) {
        callExecutor.execute {
            val arr = arrayListOf(candidate)
            sendCallMessage(MessageCategory.WEBRTC_ICE_CANDIDATE.name, gson.toJson(arr))
        }
    }

    override fun onDisconnected() {
        callExecutor.execute {
            handleCallLocalEnd()
        }
    }

    override fun onPeerConnectionClosed() {
        // TODO need keep service survive for group call
        stopService<VoiceCallService>(this)
    }

    private fun sendCallMessage(category: String, content: String? = null) {
        val quoteMessageId = callState.trackId
        val message = if (callState.isOffer) {
            val messageId = UUID.randomUUID().toString()
            val conversationId = callState.conversationId!!
            if (category == MessageCategory.WEBRTC_AUDIO_OFFER.name) {
                callState.trackId = messageId
                createCallMessage(messageId, conversationId, self.userId, category, content, nowInUtc(), MessageStatus.SENDING.name)
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
const val ACTION_CALL_DISCONNECT = "call_disconnect"

fun incomingCall(ctx: Context, user: User, data: BlazeMessageData, pendingCandidateData: String? = null) =
    startService<VoiceCallService>(ctx, ACTION_CALL_INCOMING) {
        it.putExtra(ARGS_USER, user)
        it.putExtra(EXTRA_BLAZE, data)
        if (pendingCandidateData != null) {
            it.putExtra(EXTRA_PENDING_CANDIDATES, pendingCandidateData)
        }
    }

fun outgoingCall(ctx: Context, conversationId: String, user: User? = null) =
    startService<VoiceCallService>(ctx, ACTION_CALL_OUTGOING) {
        it.putExtra(ARGS_USER, user)
        it.putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }

fun answerCall(ctx: Context, data: BlazeMessageData? = null) =
    startService<VoiceCallService>(ctx, ACTION_CALL_ANSWER) { intent ->
        data?.let {
            intent.putExtra(EXTRA_BLAZE, data)
        }
    }

fun candidate(ctx: Context, data: BlazeMessageData) =
    startService<VoiceCallService>(ctx, ACTION_CANDIDATE) {
        it.putExtra(EXTRA_BLAZE, data)
    }

fun cancelCall(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_CANCEL) {}

fun declineCall(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_DECLINE) {}

fun localEnd(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_LOCAL_END) {}

fun remoteEnd(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_REMOTE_END) {}

fun busy(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_BUSY) {}

fun remoteFailed(ctx: Context) = startService<VoiceCallService>(ctx, ACTION_CALL_REMOTE_FAILED) {}
