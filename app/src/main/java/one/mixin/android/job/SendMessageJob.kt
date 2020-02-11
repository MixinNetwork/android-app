package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import com.bugsnag.android.Bugsnag
import java.io.File
import one.mixin.android.RxBus
import one.mixin.android.crypto.Base64
import one.mixin.android.event.RecallEvent
import one.mixin.android.extension.findLastUrl
import one.mixin.android.extension.getBotNumber
import one.mixin.android.extension.getFilePath
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.Session
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.isCall
import one.mixin.android.vo.isPlain
import one.mixin.android.vo.isRecall
import one.mixin.android.vo.isText
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.ResendData
import one.mixin.android.websocket.createCallMessage
import one.mixin.android.websocket.createParamBlazeMessage

open class SendMessageJob(
    val message: Message,
    private val resendData: ResendData? = null,
    private val alreadyExistMessage: Boolean = false,
    private var recipientId: String? = null,
    private val recallMessageId: String? = null,
    messagePriority: Int = PRIORITY_SEND_MESSAGE
) : MixinJob(
    Params(messagePriority).addTags(message.id).groupBy("send_message_group")
        .requireWebSocketConnected().persist(), message.id
) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun cancel() {
        isCancel = true
        removeJob()
    }

    override fun onAdded() {
        super.onAdded()
        if (chatWebSocket.connected) {
            jobManager.start()
        }
        if (message.isCall()) {
            return
        }
        if (alreadyExistMessage) {
            return
        }
        val conversation = conversationDao.findConversationById(message.conversationId)
        if (conversation != null) {
            if (message.isRecall()) {
                recallMessage()
            } else {
                messageDao.insert(message)
                parseHyperlink()
            }
        } else {
            Bugsnag.notify(Throwable("Insert failed, no conversation $alreadyExistMessage"))
        }
    }

    private fun recallMessage() {
        messageDao.recallMessage(recallMessageId!!)
        messageDao.findMessageById(recallMessageId)?.let { msg ->
            RxBus.publish(RecallEvent(msg.id))
            messageDao.recallFailedMessage(msg.id)
            messageDao.takeUnseen(Session.getAccountId()!!, msg.conversationId)
            msg.mediaUrl?.getFilePath()?.let {
                File(it).let { file ->
                    if (file.exists() && file.isFile) {
                        file.delete()
                    }
                }
            }
            messageDao.findMessageItemById(message.conversationId, msg.id)?.let { quoteMsg ->
                messageDao.updateQuoteContentByQuoteId(
                    message.conversationId,
                    msg.id,
                    GsonHelper.customGson.toJson(quoteMsg)
                )
            }
            jobManager.cancelJobById(msg.id)
        }
    }

    private fun parseHyperlink() {
        if (message.category.endsWith("_TEXT")) {
            message.content?.findLastUrl()?.let {
                jobManager.addJobInBackground(ParseHyperlinkJob(it, message.id))
            }
        }
    }

    override fun onCancel(cancelReason: Int, throwable: Throwable?) {
        super.onCancel(cancelReason, throwable)
        removeJob()
    }

    override fun onRun() {
        jobManager.saveJob(this)
        if (message.isText()) {
            val botNumber = message.content?.getBotNumber()
            if (botNumber != null && botNumber.isNotBlank()) {
                recipientId = userDao.findUserIdByAppNumber(message.conversationId, botNumber)
                recipientId?.let {
                    message.category = MessageCategory.PLAIN_TEXT.name
                }
            }
        }
        if (message.isPlain() || message.isCall() || message.isRecall()) {
            sendPlainMessage()
        } else {
            sendSignalMessage()
        }
        removeJob()
    }

    private fun sendPlainMessage() {
        val conversation = conversationDao.getConversation(message.conversationId) ?: return
        checkConversationExist(conversation)
        var content = message.content
        if (message.category == MessageCategory.PLAIN_TEXT.name || message.isCall()) {
            if (message.content != null) {
                content = Base64.encodeBytes(message.content!!.toByteArray())
            }
        }
        val blazeParam = BlazeMessageParam(
            message.conversationId,
            recipientId,
            message.id,
            message.category,
            content,
            quote_message_id = message.quoteMessageId
        )
        val blazeMessage = if (message.isCall()) {
            createCallMessage(blazeParam)
        } else {
            createParamBlazeMessage(blazeParam)
        }
        deliver(blazeMessage)
    }

    private fun sendSignalMessage() {
        if (resendData != null) {
            if (checkSignalSession(resendData.userId, resendData.sessionId)) {
                deliver(encryptNormalMessage())
            }
            return
        }
        if (!signalProtocol.isExistSenderKey(message.conversationId, message.userId)) {
            checkConversation(message.conversationId)
        }
        checkSessionSenderKey(message.conversationId)
        deliver(encryptNormalMessage())
    }

    private fun encryptNormalMessage(): BlazeMessage {
        return if (resendData != null) {
            signalProtocol.encryptSessionMessage(
                message,
                resendData.userId,
                resendData.messageId,
                resendData.sessionId
            )
        } else {
            signalProtocol.encryptGroupMessage(message)
        }
    }
}
