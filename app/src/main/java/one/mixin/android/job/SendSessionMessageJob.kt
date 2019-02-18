package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.crypto.Base64
import one.mixin.android.util.Session
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.isPlain
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.createParamSessionMessage

class SendSessionMessageJob (
    private val message: Message,
    val userId: String? = null,
    priority: Int = PRIORITY_SEND_MESSAGE
) : MixinJob(Params(priority).addTags(message.id).groupBy("send_session_message_group").requireWebSocketConnected().persist(), message.id) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        jobManager.saveJob(this)
        if (message.isPlain()) {
            sendPlainMessage()
        } else {
            sendSignalMessage()
        }
        removeJob()
    }

    override fun cancel() {
        removeJob()
    }

    private fun sendPlainMessage() {
        var content = message.content
        if (message.category == MessageCategory.PLAIN_TEXT.name) {
            if (message.content != null) {
                content = Base64.encodeBytes(message.content!!.toByteArray())
            }
        }
        val accountId = Session.getAccountId()
        val sessionId = Session.getExtensionSession()
        val blazeParam = BlazeMessageParam(message.conversationId, message.userId,
            message.id, message.category, content, MessageStatus.SENT.name, quote_message_id = message.quoteMessageId,
            transfer_id = accountId, session_id =  sessionId)
        val blazeMessage = createParamSessionMessage(blazeParam)
        deliver(blazeMessage)
    }

    private fun sendSignalMessage() {
        val accountId = Session.getAccountId()!!
        val sessionId = Session.getExtensionSession()!!
        checkSignalSession(accountId, sessionId)
        val encrypted = signalProtocol.encryptTransferSessionMessage(message, sessionId, accountId)
        deliver(encrypted)
    }
}