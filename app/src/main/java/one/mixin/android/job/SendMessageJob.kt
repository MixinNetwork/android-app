package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import com.bugsnag.android.Bugsnag
import one.mixin.android.api.request.ConversationRequest
import one.mixin.android.api.request.ParticipantRequest
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.findLastUrl
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus.SENT
import one.mixin.android.vo.Participant
import one.mixin.android.vo.isGroup
import one.mixin.android.vo.isPlain
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.ResendData
import one.mixin.android.websocket.createParamBlazeMessage

open class SendMessageJob(
    val message: Message,
    private val resendData: ResendData? = null,
    private val alreadyExistMessage: Boolean = false,
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
        if (!alreadyExistMessage) {
            val conversation = conversationDao.findConversationById(message.conversationId)
            if (conversation != null) {
                messageDao.insert(message)
                parseHyperlink()
            } else {
                Bugsnag.notify(Throwable("Insert failed, no conversation $alreadyExistMessage"))
            }
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
        if (message.isPlain()) {
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
        if (message.category == MessageCategory.PLAIN_TEXT.name) {
            content = Base64.encodeBytes(message.content!!.toByteArray())
        }
        val blazeParam = BlazeMessageParam(message.conversationId, null,
            message.id, message.category, content, SENT.name)
        deliver(createParamBlazeMessage(blazeParam))
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
            deliver(encryptNormalMessage())
        } else {
            val conversation = conversationDao.getConversation(message.conversationId)!!
            if (conversation.isGroup()) {
                syncConversation(conversation)
                sendGroupSenderKey(conversation.conversationId)
            } else {
                requestCreateConversation(conversation)
                sendSenderKey(conversation.conversationId, conversation.ownerId!!)
            }
            deliver(encryptNormalMessage())
        }
    }

    private fun requestCreateConversation(conversation: Conversation) {
        if (conversation.status != ConversationStatus.SUCCESS.ordinal) {
            val participantRequest = arrayListOf(ParticipantRequest(conversation.ownerId!!, ""))
            val request = ConversationRequest(conversationId = conversation.conversationId,
                category = conversation.category, participants = participantRequest)
            val response = conversationApi.create(request).execute().body()
            if (response != null && response.isSuccess && response.data != null && !isCancel) {
                conversationDao
                    .updateConversationStatusById(conversation.conversationId, ConversationStatus.SUCCESS.ordinal)
            } else {
                throw Exception("Create Conversation Exception")
            }
        }
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