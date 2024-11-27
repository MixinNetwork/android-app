package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.Constants.DEFAULT_THUMB_IMAGE
import one.mixin.android.Constants.MAX_THUMB_IMAGE_LENGTH
import one.mixin.android.RxBus
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.db.insertMessage
import one.mixin.android.event.RecallEvent
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.base64RawURLDecode
import one.mixin.android.extension.currentTimeSeconds
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.findLastUrl
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.fts.deleteByMessageId
import one.mixin.android.fts.insertOrReplaceMessageFts4
import one.mixin.android.session.Session
import one.mixin.android.util.GsonHelper
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
    messagePriority: Int = PRIORITY_SEND_MESSAGE,
) : MixinJob(Params(messagePriority).groupBy("send_message_group").requireWebSocketConnected().persist(), message.messageId) {
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
        val conversation = conversationDao().findConversationById(message.conversationId)
        if (conversation != null) {
            if (message.isRecall()) {
                recallMessage(message.conversationId)
            } else if (!message.isPin()) {
                if (message.isText()) {
                    message.content?.let { content ->
                        content.findLastUrl()?.let {
                            message.hyperlink = it
                            parseHyperlink(it, hyperlinkDao())
                        }
                        parseMentionData(content, message.messageId, message.conversationId, userDao(), messageMentionDao(), message.userId)
                    }
                }
                if (!message.isTranscript()) {
                    database().insertMessage(message)
                    MessageFlow.insert(message.conversationId, message.messageId)
                    ftsDatabase().insertOrReplaceMessageFts4(message)
                }

                conversation.expireIn?.let { e ->
                    if (e > 0) {
                        expiredMessageDao().insert(
                            ExpiredMessage(
                                message.messageId,
                                e,
                                null,
                            ),
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
        messageDao().findMessageById(recallMessageId)?.let { msg ->
            RxBus.publish(RecallEvent(msg.messageId))
            messageDao().recallFailedMessage(msg.messageId)
            messageDao().recallMessage(msg.messageId)
            messageDao().recallPinMessage(msg.messageId, msg.conversationId)
            pinMessageDao().deleteByMessageId(msg.messageId)
            messageMentionDao().deleteMessage(msg.messageId)
            remoteMessageStatusDao().deleteByMessageId(recallMessageId)
            remoteMessageStatusDao().updateConversationUnseen(msg.conversationId)
            msg.mediaUrl?.getFilePath()?.let {
                File(it).let { file ->
                    if (file.exists() && file.isFile) {
                        file.delete()
                    }
                }
            }

            messageDao().findQuoteMessageItemById(message.conversationId, msg.messageId)?.let { quoteMsg ->
                quoteMsg.thumbImage =
                    if ((quoteMsg.thumbImage?.length ?: 0) > MAX_THUMB_IMAGE_LENGTH) {
                        DEFAULT_THUMB_IMAGE
                    } else {
                        quoteMsg.thumbImage
                    }
                messageDao().updateQuoteContentByQuoteId(
                    message.conversationId,
                    msg.messageId,
                    GsonHelper.customGson.toJson(quoteMsg),
                )
            }
            MessageFlow.update(conversationId, messageDao().findQuoteMessageIdByQuoteId(conversationId, recallMessageId))
            jobManager.cancelJobByMixinJobId(msg.messageId)
        }
        MessageFlow.update(conversationId, recallMessageId)
        ftsDatabase().deleteByMessageId(recallMessageId)
    }

    override fun onCancel(
        cancelReason: Int,
        throwable: Throwable?,
    ) {
        super.onCancel(cancelReason, throwable)
        removeJob()
    }

    override fun onRun() {
        if (isCancelled) {
            removeJob()
            return
        }
        jobManager.saveJob(this)
        val conversation = conversationDao().findConversationById(message.conversationId)
        val expiredMessageCallback = fun(expireIn: Long?) {
            expireIn?.let { e -> // Update local expiration time after success
                if (expireIn > 0) {
                    expiredMessageDao().updateExpiredMessage(
                        message.messageId,
                        currentTimeSeconds() + e,
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

    private fun sendPlainMessage(
        conversation: Conversation?,
        callback: (Long?) -> Unit,
    ) {
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
        val blazeParam =
            BlazeMessageParam(
                message.conversationId,
                recipientId,
                message.messageId,
                message.category,
                content,
                quote_message_id = message.quoteMessageId,
                mentions = getMentionData(message.messageId),
                recipient_ids = recipientIds,
                silent = isSilent,
                expire_in = expireIn,
            )
        val blazeMessage =
            if (message.isCall()) {
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

    private fun sendEncryptedMessage(
        conversation: Conversation?,
        callback: (Long?) -> Unit,
    ) {
        conversation ?: return
        val expireIn = checkConversationExist(conversation)
        val accountId = Session.getAccountId()!!
        var participantSessionKey = getBotSessionKey(accountId)
        if (participantSessionKey == null || participantSessionKey.publicKey.isNullOrBlank()) {
            jobSenderKey.syncConversation(message.conversationId)
            participantSessionKey = getBotSessionKey(accountId)
        }
        // Workaround No session key, can't encrypt message, send PLAIN directly
        if (participantSessionKey?.publicKey == null) {
            message.category = message.category.replace("ENCRYPTED_", "PLAIN_")
            messageDao().updateCategoryById(message.messageId, message.category)
            sendPlainMessage(conversation, callback)
            return
        }

        val extensionSessionKey =
            Session.getExtensionSessionId().notNullWithElse({ participantSessionDao().getParticipantSessionKeyBySessionId(message.conversationId, accountId, it) }, null)

        val keyPair = Session.getEd25519KeyPair() ?: return
        val plaintext =
            if (message.isAttachment() || message.isSticker() || message.isContact()) {
                message.content!!.decodeBase64()
            } else {
                message.content!!.toByteArray()
            }
        val encryptContent =
            encryptedProtocol.encryptMessage(
                keyPair,
                plaintext,
                participantSessionKey.publicKey!!.base64RawURLDecode(),
                participantSessionKey.sessionId,
                extensionSessionKey?.publicKey?.base64RawURLDecode(),
                extensionSessionKey?.sessionId,
            )

        val blazeParam =
            BlazeMessageParam(
                message.conversationId,
                recipientId,
                message.messageId,
                message.category,
                encryptContent.base64Encode(),
                quote_message_id = message.quoteMessageId,
                mentions = getMentionData(message.messageId),
                recipient_ids = recipientIds,
                expire_in = expireIn,
            )
        val blazeMessage = createParamBlazeMessage(blazeParam)
        deliver(blazeMessage)
        callback(expireIn)
    }

    private fun getBotSessionKey(accountId: String): ParticipantSessionKey? =
        if (recipientId != null) {
            participantSessionDao().getParticipantSessionKeyByUserId(
                message.conversationId,
                recipientId!!,
            )
        } else {
            participantSessionDao().getParticipantSessionKeyWithoutSelf(
                message.conversationId,
                accountId,
            )
        }

    private fun sendSignalMessage(
        conversation: Conversation?,
        callback: (Long?) -> Unit,
    ) {
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
        jobSenderKey.checkSessionSenderKey(message.conversationId)
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
                getMentionData(message.messageId),
                expireIn,
            )
        } else {
            signalProtocol.encryptGroupMessage(message, getMentionData(message.messageId), isSilent, expireIn)
        }
    }

    private fun getMentionData(messageId: String): List<String>? {
        return messageMentionDao().getMentionData(messageId)?.run {
            GsonHelper.customGson.fromJson(this, Array<MentionUser>::class.java).map {
                it.identityNumber
            }.toSet()
        }?.run {
            userDao().findMultiUserIdsByIdentityNumbers(this)
        }
    }
}
