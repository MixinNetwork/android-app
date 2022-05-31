package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.RxBus
import one.mixin.android.event.RecallEvent
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.base64RawUrlDecode
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.findLastUrl
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.session.Session
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.MessageFts4Helper
import one.mixin.android.util.chat.InvalidateFlow
import one.mixin.android.util.hyperlink.parseHyperlink
import one.mixin.android.util.mention.parseMentionData
import one.mixin.android.util.reportException
import one.mixin.android.vo.Conversation
import one.mixin.android.vo.ExpiredMessage
import one.mixin.android.vo.MentionUser
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.ParticipantSessionKey
import one.mixin.android.vo.isAttachment
import one.mixin.android.vo.isCall
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isEncrypted
import one.mixin.android.vo.isKraken
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isPin
import one.mixin.android.vo.isPlain
import one.mixin.android.vo.isRecall
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.BlazeMessageParam
import one.mixin.android.websocket.KrakenParam
import one.mixin.android.websocket.ResendData
import one.mixin.android.websocket.createCallMessage
import one.mixin.android.websocket.createKrakenMessage
import one.mixin.android.websocket.createParamBlazeMessage
import java.io.File

open class SendMessageJob(
    val message: Message,
    private val resendData: ResendData? = null,
    private val alreadyExistMessage: Boolean = false,
    private var recipientId: String? = null,
    private var recipientIds: List<String>? = null,
    private val recallMessageId: String? = null,
    private val krakenParam: KrakenParam? = null,
    private val isSilent: Boolean? = null,
    messagePriority: Int = PRIORITY_SEND_MESSAGE
) : MixinJob(Params(messagePriority).groupBy("send_message_group").requireWebSocketConnected().persist(), message.id) {

    companion object {
        private const val serialVersionUID = 1L
    }

    override fun cancel() {
        isCancelled = true
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
                recallMessage(message.conversationId)
            } else if (!message.isPin()) {
                if (message.isText()) {
                    message.content?.let { content ->
                        content.findLastUrl()?.let {
                            message.hyperlink = it
                            parseHyperlink(message.id, it, hyperlinkDao, messageDao)
                        }
                        parseMentionData(content, message.id, message.conversationId, userDao, messageMentionDao, message.userId)
                    }
                }
                if (!message.isTranscript()) {
                    messageDao.insert(message)
                    InvalidateFlow.emit(message.conversationId)
                    MessageFts4Helper.insertOrReplaceMessageFts4(message, message.name)
                }

                conversation.expireIn?.let { e ->
                    if (e > 0) {
                        expiredMessageDao.insert(
                            ExpiredMessage(
                                message.id,
                                e,
                                null
                            )
                        )
                    }
                }
            }
        } else {
            reportException(Throwable("Insert failed, no conversation $alreadyExistMessage"))
        }
    }

    private fun recallMessage(conversationId: String) {
        recallMessageId ?: return
        messageDao.findMessageById(recallMessageId)?.let { msg ->
            RxBus.publish(RecallEvent(msg.id))
            messageDao.recallFailedMessage(msg.id)
            messageDao.recallMessage(msg.id)
            messageDao.recallPinMessage(msg.id, msg.conversationId)
            pinMessageDao.deleteByMessageId(msg.id)
            messageMentionDao.deleteMessage(msg.id)
            remoteMessageStatusDao.deleteByMessageId(recallMessageId)
            remoteMessageStatusDao.updateConversationUnseen(msg.conversationId)
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
            jobManager.cancelJobByMixinJobId(msg.id)
        }
        InvalidateFlow.emit(conversationId)
        messageFts4Dao.deleteByMessageId(recallMessageId)
    }

    override fun onCancel(cancelReason: Int, throwable: Throwable?) {
        super.onCancel(cancelReason, throwable)
        removeJob()
    }

    override fun onRun() {
        if (isCancelled) {
            removeJob()
            return
        }
        jobManager.saveJob(this)
        val conversation = conversationDao.findConversationById(message.conversationId)
        val expiredMessageCallback = fun(expireIn: Long?) {
            expireIn?.let { e -> // Update local expiration time after success
                if (expireIn > 0) {
                    expiredMessageDao.updateExpiredMessage(
                        message.id,
                        currentTimeSeconds() + e
                    )
                }
            }
        }
        if (message.isPlain() || message.isCall() || message.isRecall() || message.isPin() || message.category == MessageCategory.APP_CARD.name) {
            sendPlainMessage(conversation, expiredMessageCallback)
        } else if (message.isEncrypted()) {
            sendEncryptedMessage(conversation, expiredMessageCallback)
        } else if (message.isSignal()) {
            sendSignalMessage(conversation, expiredMessageCallback)
        }

        removeJob()
    }

    private fun sendPlainMessage(conversation: Conversation?, callback: (Long?) -> Unit) {
        conversation ?: return
        val expireIn = checkConversationExist(conversation)
        var content = message.content
        if (message.category == MessageCategory.PLAIN_TEXT.name ||
            message.category == MessageCategory.PLAIN_POST.name ||
            message.category == MessageCategory.PLAIN_TRANSCRIPT.name ||
            message.category == MessageCategory.PLAIN_LIVE.name ||
            message.category == MessageCategory.PLAIN_LOCATION.name ||
            message.isCall() ||
            message.category == MessageCategory.APP_CARD.name
        ) {
            if (message.content != null) {
                content = message.content!!.base64Encode()
            }
        }
        val blazeParam = BlazeMessageParam(
            message.conversationId,
            recipientId,
            message.id,
            message.category,
            content,
            quote_message_id = message.quoteMessageId,
            mentions = getMentionData(message.id),
            recipient_ids = recipientIds,
            silent = isSilent,
            expire_in = expireIn
        )
        val blazeMessage = if (message.isCall()) {
            if (message.isKraken()) {
                blazeParam.jsep = krakenParam?.jsep?.base64Encode()
                blazeParam.candidate = krakenParam?.candidate?.base64Encode()
                blazeParam.track_id = krakenParam?.track_id
                createKrakenMessage(blazeParam)
            } else {
                createCallMessage(blazeParam)
            }
        } else {
            createParamBlazeMessage(blazeParam)
        }
        deliver(blazeMessage)
        callback(expireIn)
    }

    @ExperimentalUnsignedTypes
    private fun sendEncryptedMessage(conversation: Conversation?, callback: (Long?) -> Unit) {
        conversation ?: return
        val expireIn = checkConversationExist(conversation)
        val accountId = Session.getAccountId()!!
        var participantSessionKey = getBotSessionKey(accountId)
        if (participantSessionKey == null || participantSessionKey.publicKey.isNullOrBlank()) {
            syncConversation(message.conversationId)
            participantSessionKey = getBotSessionKey(accountId)
        }
        // Workaround No session key, can't encrypt message, send PLAIN directly
        if (participantSessionKey?.publicKey == null) {
            message.category = message.category.replace("ENCRYPTED_", "PLAIN_")
            messageDao.updateCategoryById(message.id, message.category)
            sendPlainMessage(conversation, callback)
            return
        }

        val extensionSessionKey =
            Session.getExtensionSessionId().notNullWithElse({ participantSessionDao.getParticipantSessionKeyBySessionId(message.conversationId, accountId, it) }, null)

        val privateKey = Session.getEd25519PrivateKey() ?: return
        val plaintext = if (message.isAttachment() || message.isSticker() || message.isContact()) {
            message.content!!.decodeBase64()
        } else {
            message.content!!.toByteArray()
        }
        val encryptContent = encryptedProtocol.encryptMessage(
            privateKey,
            plaintext,
            participantSessionKey.publicKey!!.base64RawUrlDecode(),
            participantSessionKey.sessionId,
            extensionSessionKey?.publicKey?.base64RawUrlDecode(),
            extensionSessionKey?.sessionId
        )

        val blazeParam = BlazeMessageParam(
            message.conversationId,
            recipientId,
            message.id,
            message.category,
            encryptContent.base64Encode(),
            quote_message_id = message.quoteMessageId,
            mentions = getMentionData(message.id),
            recipient_ids = recipientIds,
            expire_in = expireIn
        )
        val blazeMessage = createParamBlazeMessage(blazeParam)
        deliver(blazeMessage)
        callback(expireIn)
    }

    private fun getBotSessionKey(accountId: String): ParticipantSessionKey? =
        if (recipientId != null) {
            participantSessionDao.getParticipantSessionKeyByUserId(
                message.conversationId,
                recipientId!!
            )
        } else {
            participantSessionDao.getParticipantSessionKeyWithoutSelf(
                message.conversationId,
                accountId
            )
        }

    private fun sendSignalMessage(conversation: Conversation?, callback: (Long?) -> Unit) {
        conversation ?: return
        val expireIn = checkConversationExist(conversation)
        if (resendData != null) {
            if (checkSignalSession(resendData.userId, resendData.sessionId)) {
                deliver(encryptNormalMessage(expireIn))
                callback(expireIn)
            }
            return
        }
        if (!signalProtocol.isExistSenderKey(message.conversationId, message.userId)) {
            checkConversation(message.conversationId)
        }
        checkSessionSenderKey(message.conversationId)
        deliver(encryptNormalMessage(expireIn))
        callback(expireIn)
    }

    private fun encryptNormalMessage(expireIn: Long?): BlazeMessage {
        if (message.isLive()) {
            message.content = message.content?.base64Encode()
        }
        return if (resendData != null) {
            signalProtocol.encryptSessionMessage(
                message,
                resendData.userId,
                resendData.messageId,
                resendData.sessionId,
                getMentionData(message.id),
                expireIn
            )
        } else {
            signalProtocol.encryptGroupMessage(message, getMentionData(message.id), isSilent, expireIn)
        }
    }

    private fun getMentionData(messageId: String): List<String>? {
        return messageMentionDao.getMentionData(messageId)?.run {
            GsonHelper.customGson.fromJson(this, Array<MentionUser>::class.java).map {
                it.identityNumber
            }.toSet()
        }?.run {
            userDao.findMultiUserIdsByIdentityNumbers(this)
        }
    }
}
