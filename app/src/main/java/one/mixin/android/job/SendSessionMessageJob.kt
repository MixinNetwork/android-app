package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.crypto.Base64
import one.mixin.android.util.Session
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.SYSTEM_USER
import one.mixin.android.vo.isPlain
import one.mixin.android.vo.isSignal
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.createParamSessionMessage
import java.util.*

class SendSessionMessageJob(
    private val message: Message,
    val content: String? = null,
    priority: Int = PRIORITY_SEND_MESSAGE
) : MixinJob(Params(priority).addTags(message.id).groupBy("send_session_message_group").requireWebSocketConnected().persist(), message.id) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        jobManager.saveJob(this)
        val accountId = Session.getAccountId()!!
        val sessionId = Session.getExtensionSessionId()!!
        when {
            message.isPlain() -> sendPlainMessage(accountId, sessionId)
            message.isSignal() -> sendSignalMessage(accountId, sessionId)
            message.category.startsWith("SYSTEM_") -> sendPlainMessage(accountId, sessionId)
        }
        removeJob()
    }

    override fun cancel() {
        removeJob()
    }

    private fun sendPlainMessage(accountId: String, sessionId: String) {
        var content = message.content
        if (message.category == MessageCategory.PLAIN_TEXT.name) {
            if (message.content != null) {
                content = Base64.encodeBytes(message.content!!.toByteArray())
            }
        }

        val primitiveId = if (message.category.startsWith("SYSTEM_")) {
            SYSTEM_USER
        } else {
            message.userId
        }

        val blazeParam = BlazeMessageParam(message.conversationId,
            accountId,
            UUID.randomUUID().toString(),
            message.category,
            content,
            quote_message_id = message.quoteMessageId,
            primitive_id = primitiveId,
            primitive_message_id = message.id,
            session_id = sessionId)
        val blazeMessage = createParamSessionMessage(blazeParam)
        deliver(blazeMessage)
    }

    private fun sendSignalMessage(accountId: String, sessionId: String) {
        checkSignalSession(accountId, sessionId)
        if (content != null) {
            message.content = content
        }
        val encrypted = signalProtocol.encryptTransferSessionMessage(message, sessionId, accountId)
        deliver(encrypted)
    }
}