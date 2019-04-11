package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import com.bugsnag.android.Bugsnag
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.findLastUrl
import one.mixin.android.util.Session
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.Participant
import one.mixin.android.vo.isCall
import one.mixin.android.vo.isGroup
import one.mixin.android.vo.isPlain
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.ResendData
import one.mixin.android.websocket.createCallMessage
import one.mixin.android.websocket.createParamBlazeMessage

open class SendMessageJob(
    val message: Message,
    private val resendData: ResendData? = null,
    private val alreadyExistMessage: Boolean = false,
    private val recipientId: String? = null,
    messagePriority: Int = PRIORITY_SEND_MESSAGE
) : MixinJob(Params(messagePriority).addTags(message.id).groupBy("send_message_group")
    .requireWebSocketConnected().persist(), message.id) {

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
            messageDao.insert(message)
            parseHyperlink()
        } else {
            Bugsnag.notify(Throwable("Insert failed, no conversation $alreadyExistMessage"))
        }

        if (Session.getExtensionSessionId() != null) {
            jobManager.addJobInBackground(SendSessionMessageJob(message))
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
        if (message.isPlain() || message.isCall()) {
            sendPlainMessage()
        } else {
            sendSignalMessage()
        }
        removeJob()
    }

    private fun sendPlainMessage() {
        val conversation = conversationDao.getConversation(message.conversationId) ?: return
        requestCreateConversation(conversation)
        var content = message.content
        if (message.category == MessageCategory.PLAIN_TEXT.name || message.isCall()) {
            if (message.content != null) {
                content = Base64.encodeBytes(message.content!!.toByteArray())
            }
        }
        val blazeParam = BlazeMessageParam(message.conversationId, recipientId,
            message.id, message.category, content, quote_message_id = message.quoteMessageId)
        val blazeMessage = if (message.isCall()) {
            createCallMessage(blazeParam)
        } else {
            createParamBlazeMessage(blazeParam)
        }
        deliver(blazeMessage)
    }

    private fun sendSignalMessage() {
        if (resendData != null) {
            if (checkSignalSession(resendData.userId)) {
                deliver(encryptNormalMessage())
            }
            return
        }
        if (signalProtocol.isExistSenderKey(message.conversationId, message.userId)) {
            checkSentSenderKey(message.conversationId)
        } else {
            val conversation = conversationDao.getConversation(message.conversationId) ?: return
            if (conversation.isGroup()) {
                syncConversation(conversation)
                sendGroupSenderKey(conversation.conversationId)
            } else {
                requestCreateConversation(conversation)
                sendSenderKey(conversation.conversationId, conversation.ownerId!!)
            }
        }
        deliver(encryptNormalMessage())
    }

    private fun syncConversation(conversation: Conversation) {
        val local = participantDao.getRealParticipants(conversation.conversationId)
        val localIds = local.map { it.userId }
        val response = conversationApi.getConversation(conversation.conversationId).execute().body()
        if (response != null && response.isSuccess) {
            response.data?.let { data ->
                val remote = data.participants.map {
                    Participant(conversation.conversationId, it.userId, it.role, it.createdAt!!)
                }
                val remoteIds = remote.map { it.userId }
                val needAdd = remote.filter { !localIds.contains(it.userId) }
                val needRemove = local.filter { !remoteIds.contains(it.userId) }
                if (needRemove.isNotEmpty()) {
                    participantDao.deleteList(needRemove)
                }
                if (needAdd.isNotEmpty()) {
                    participantDao.insertList(needAdd)
                }
            }
        }
    }

    private fun encryptNormalMessage(): BlazeMessage {
        return if (resendData != null) {
            signalProtocol.encryptSessionMessage(message, resendData.userId, resendData.messageId)
        } else {
            signalProtocol.encryptGroupMessage(message)
        }
    }
}