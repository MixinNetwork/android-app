package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.crypto.Base64
import one.mixin.android.util.Session
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.SYSTEM_USER
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isPlain
import one.mixin.android.vo.isRecall
import one.mixin.android.vo.isSignal
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.createParamSessionMessage
import java.util.UUID

class SendSessionMessageJob(
    private val message: Message,
    private val content: String? = null,
    private val dataUserId: String? = null,
    priority: Int = PRIORITY_SEND_SESSION_MESSAGE
) : MixinJob(Params(priority).addTags(message.id).groupBy("send_session_message_group").requireWebSocketConnected().persist(), message.id) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        val sessionId = Session.getExtensionSessionId()
        if (sessionId == null) {
            removeJob()
            return
        }

        val conversation = conversationDao.getConversation(message.conversationId) ?: return
        if (conversation.isContact()) {
            requestCreateConversation(conversation)
        }
        jobManager.saveJob(this)
        val accountId = Session.getAccountId()!!
        if (content != null) {
            message.content = content
        }
        when {
            message.isPlain() || message.isRecall() -> sendPlainMessage(accountId, sessionId)
            message.isSignal() -> sendSignalMessage(accountId, sessionId)
            message.category.startsWith("SYSTEM_") -> sendPlainMessage(accountId, sessionId)
            message.category.startsWith("APP_") -> sendPlainMessage(accountId, sessionId)
        }
        removeJob()
    }

    override fun cancel() {
        removeJob()
    }

    private fun sendPlainMessage(accountId: String, sessionId: String) {
        var data = message.content
        if (message.category == MessageCategory.PLAIN_TEXT.name) {
            if (message.content != null) {
                data = Base64.encodeBytes(message.content!!.toByteArray())
            }
        }

        val primitiveId = if (message.category == MessageCategory.SYSTEM_CONVERSATION.name) {
            SYSTEM_USER
        } else {
            message.userId
        }

        val blazeParam = BlazeMessageParam(message.conversationId,
            accountId,
            UUID.randomUUID().toString(),
            message.category,
            data,
            quote_message_id = message.quoteMessageId,
            primitive_id = primitiveId,
            primitive_message_id = message.id,
            session_id = sessionId)

        if (dataUserId != null) {
            blazeParam.primitive_id = dataUserId
            blazeParam.representative_id = message.userId
        }

        val blazeMessage = createParamSessionMessage(blazeParam)
        deliver(blazeMessage)
    }

    private fun sendSignalMessage(accountId: String, sessionId: String) {
        val result = checkSignalSession(accountId, sessionId)
        if (!result) {
            return
        }
        val encrypted = signalProtocol.encryptTransferSessionMessage(message, sessionId, accountId)
        deliver(encrypted)
    }
}