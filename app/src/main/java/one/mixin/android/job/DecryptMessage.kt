package one.mixin.android.job

import android.app.Activity
import android.app.NotificationManager
import androidx.collection.arrayMapOf
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.api.handleMixinResponse
import one.mixin.android.api.response.SignalKeyCount
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.vo.RatchetSenderKey
import one.mixin.android.crypto.vo.RatchetStatus
import one.mixin.android.db.insertAndNotifyConversation
import one.mixin.android.db.insertNoReplace
import one.mixin.android.db.insertUpdate
import one.mixin.android.db.runInTransaction
import one.mixin.android.event.CircleDeleteEvent
import one.mixin.android.event.PinMessageEvent
import one.mixin.android.event.RecallEvent
import one.mixin.android.event.SenderKeyChange
import one.mixin.android.extension.autoDownload
import one.mixin.android.extension.autoDownloadDocument
import one.mixin.android.extension.autoDownloadPhoto
import one.mixin.android.extension.autoDownloadVideo
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.findLastUrl
import one.mixin.android.extension.getDeviceId
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.joinWhiteSpace
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.postOptimize
import one.mixin.android.extension.putString
import one.mixin.android.extension.toByteArray
import one.mixin.android.job.BaseJob.Companion.PRIORITY_SEND_ATTACHMENT_MESSAGE
import one.mixin.android.session.Session
import one.mixin.android.ui.web.replaceApp
import one.mixin.android.util.ColorUtil
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.MessageFts4Helper
import one.mixin.android.util.chat.InvalidateFlow
import one.mixin.android.util.hyperlink.parseHyperlink
import one.mixin.android.util.mention.parseMentionData
import one.mixin.android.util.reportException
import one.mixin.android.vo.AppButtonData
import one.mixin.android.vo.AppCap
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.CircleConversation
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageHistory
import one.mixin.android.vo.MessageMention
import one.mixin.android.vo.MessageMentionStatus
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ParticipantSession
import one.mixin.android.vo.PinMessage
import one.mixin.android.vo.PinMessageMinimal
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.ResendSessionMessage
import one.mixin.android.vo.SYSTEM_USER
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.TranscriptMinimal
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.createAttachmentMessage
import one.mixin.android.vo.createAudioMessage
import one.mixin.android.vo.createContactMessage
import one.mixin.android.vo.createLiveMessage
import one.mixin.android.vo.createLocationMessage
import one.mixin.android.vo.createMediaMessage
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.createPinMessage
import one.mixin.android.vo.createPostMessage
import one.mixin.android.vo.createReplyTextMessage
import one.mixin.android.vo.createStickerMessage
import one.mixin.android.vo.createSystemUser
import one.mixin.android.vo.createTranscriptMessage
import one.mixin.android.vo.createVideoMessage
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.isAttachment
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isIllegalMessageCategory
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.mediaDownloaded
import one.mixin.android.vo.toJson
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.AttachmentMessagePayload
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.websocket.LIST_PENDING_MESSAGES
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.websocket.PinAction
import one.mixin.android.websocket.PinMessagePayload
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.PlainJsonMessagePayload
import one.mixin.android.websocket.RecallMessagePayload
import one.mixin.android.websocket.ResendData
import one.mixin.android.websocket.StickerMessagePayload
import one.mixin.android.websocket.SystemCircleMessageAction
import one.mixin.android.websocket.SystemCircleMessagePayload
import one.mixin.android.websocket.SystemConversationAction
import one.mixin.android.websocket.SystemConversationMessagePayload
import one.mixin.android.websocket.SystemSessionMessageAction
import one.mixin.android.websocket.SystemSessionMessagePayload
import one.mixin.android.websocket.SystemUserMessageAction
import one.mixin.android.websocket.SystemUserMessagePayload
import one.mixin.android.websocket.checkLocationData
import one.mixin.android.websocket.createCountSignalKeys
import one.mixin.android.websocket.createParamBlazeMessage
import one.mixin.android.websocket.createPlainJsonParam
import one.mixin.android.websocket.createSyncSignalKeys
import one.mixin.android.websocket.createSyncSignalKeysParam
import one.mixin.android.websocket.invalidData
import org.threeten.bp.ZonedDateTime
import org.whispersystems.libsignal.NoSessionException
import org.whispersystems.libsignal.SignalProtocolAddress
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID

class DecryptMessage(private val lifecycleScope: CoroutineScope) : Injector() {

    companion object {
        val TAG = DecryptMessage::class.java.simpleName
        const val GROUP = "DecryptMessage"
    }

    private var refreshKeyMap = arrayMapOf<String, Long?>()
    private val gson = GsonHelper.customGson

    private val accountId = Session.getAccountId()

    fun onRun(data: BlazeMessageData) {
        if (isExistMessage(data.messageId)) {
            updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
            return
        }
        processMessage(data)
    }

    private fun processMessage(data: BlazeMessageData) {
        try {
            if (data.category.isIllegalMessageCategory()) {
                if (data.conversationId != SYSTEM_USER && data.conversationId != Session.getAccountId()) {
                    val message = createMessage(
                        data.messageId,
                        data.conversationId,
                        data.userId,
                        data.category,
                        data.data,
                        data.createdAt,
                        MessageStatus.UNKNOWN.name
                    )
                    syncConversation(data)
                    database.insertAndNotifyConversation(message)
                }
                updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
                return
            }

            if (data.category != MessageCategory.SYSTEM_CONVERSATION.name) {
                syncConversation(data)
            }
            checkSession(data)
            if (data.category.startsWith("SYSTEM_")) {
                processSystemMessage(data)
            } else if (data.category.startsWith("PLAIN_")) {
                processPlainMessage(data)
            } else if (data.category.startsWith("SIGNAL_")) {
                processSignalMessage(data)
            } else if (data.category.startsWith("ENCRYPTED_")) {
                processEncryptedMessage(data)
            } else if (data.category == MessageCategory.APP_BUTTON_GROUP.name) {
                processAppButton(data)
            } else if (data.category == MessageCategory.APP_CARD.name) {
                processAppCard(data)
            } else if (data.category == MessageCategory.MESSAGE_PIN.name) {
                processPinMessage(data)
            } else if (data.category == MessageCategory.MESSAGE_RECALL.name) {
                processRecallMessage(data)
            }
        } catch (e: Exception) {
            Timber.e("Process error: $e")
            insertInvalidMessage(data)
            updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
        }
    }

    private fun checkSession(data: BlazeMessageData) {
        if (data.conversationId == SYSTEM_USER || data.conversationId == Session.getAccountId() || data.userId == SYSTEM_USER) {
            return
        }
        val p = participantSessionDao.getParticipantSession(data.conversationId, data.userId, data.sessionId)
        if (p == null) {
            participantSessionDao.insert(ParticipantSession(data.conversationId, data.userId, data.sessionId))
        }
    }

    private fun processAppButton(data: BlazeMessageData) {
        val message = createMessage(
            data.messageId,
            data.conversationId,
            data.userId,
            data.category,
            String(Base64.decode(data.data)),
            data.createdAt,
            data.status
        )
        val appButton = gson.fromJson(message.content, Array<AppButtonData>::class.java)
        for (item in appButton) {
            ColorUtil.parseColor(item.color.trim())
        }
        database.insertAndNotifyConversation(message)
        updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
        generateNotification(message, data)
    }

    private fun processAppCard(data: BlazeMessageData) {
        if (!data.representativeId.isNullOrBlank()) {
            data.userId = data.representativeId
        }
        syncUser(data.userId)
        val message = createMessage(
            data.messageId,
            data.conversationId,
            data.userId,
            data.category,
            String(Base64.decode(data.data)),
            data.createdAt,
            data.status
        )
        val appCardData = gson.fromJson(message.content, AppCardData::class.java)
        appCardData.appId?.let { id ->
            runBlocking {
                var app = appDao.findAppById(id)
                if (app?.updatedAt != appCardData.updatedAt) {
                    app = handleMixinResponse(
                        invokeNetwork = {
                            userApi.getUserByIdSuspend(id)
                        },
                        successBlock = {
                            it.data?.let { u ->
                                userDao.insertUpdate(u, appDao)
                                u.app?.let { app -> replaceApp(app) }
                                return@handleMixinResponse u.app
                            }
                        }
                    )
                    if (app == null) {
                        jobManager.addJobInBackground(RefreshUserJob(listOf(id)))
                    }
                }
            }
        }
        database.insertAndNotifyConversation(message)
        updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
        generateNotification(message, data)
    }

    private fun processSystemMessage(data: BlazeMessageData) {
        if (data.category == MessageCategory.SYSTEM_CONVERSATION.name) {
            val json = Base64.decode(data.data)
            val systemMessage = gson.fromJson(String(json), SystemConversationMessagePayload::class.java)
            if (systemMessage.action != SystemConversationAction.UPDATE.name) {
                syncConversation(data)
            }
            processSystemConversationMessage(data, systemMessage)
        } else if (data.category == MessageCategory.SYSTEM_USER.name) {
            val json = Base64.decode(data.data)
            val systemMessage = gson.fromJson(String(json), SystemUserMessagePayload::class.java)
            processSystemUserMessage(systemMessage)
        } else if (data.category == MessageCategory.SYSTEM_CIRCLE.name) {
            val json = Base64.decode(data.data)
            val systemMessage = gson.fromJson(String(json), SystemCircleMessagePayload::class.java)
            processSystemCircleMessage(data, systemMessage)
        } else if (data.category == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name) {
            val json = Base64.decode(data.data)
            val systemSnapshot = gson.fromJson(String(json), Snapshot::class.java)
            processSystemSnapshotMessage(data, systemSnapshot)
        } else if (data.category == MessageCategory.SYSTEM_SESSION.name) {
            val json = Base64.decode(data.data)
            val systemSession = gson.fromJson(String(json), SystemSessionMessagePayload::class.java)
            processSystemSessionMessage(systemSession)
        }

        updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
    }

    private val notificationManager: NotificationManager by lazy {
        MixinApplication.appContext.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun processPinMessage(data: BlazeMessageData) {
        if (data.category == MessageCategory.MESSAGE_PIN.name) {
            val decoded = Base64.decode(data.data)
            val transferPinData = gson.fromJson(String(decoded), PinMessagePayload::class.java)
            if (transferPinData.action == PinAction.PIN.name) {
                transferPinData.messageIds.forEachIndexed { index, messageId ->
                    val message = messageDao.findMessageById(messageId)
                    if (message != null) {
                        pinMessageDao.insert(PinMessage(messageId, message.conversationId, data.createdAt))
                        val mid = UUID.randomUUID().toString()
                        messageDao.insert(
                            createPinMessage(
                                mid,
                                data.conversationId,
                                data.userId,
                                messageId, // quote pinned message id
                                PinMessageMinimal(
                                    message.id,
                                    message.category,
                                    if (message.category.endsWith("_TEXT")) {
                                        message.content
                                    } else {
                                        null
                                    }
                                ),
                                nowInUtc(),
                                MessageStatus.READ.name
                            )
                        )
                        InvalidateFlow.emit(data.conversationId)
                        if (message.category.endsWith("_TEXT")) {
                            messageMentionDao.findMessageMentionById(message.id)?.let { mention ->
                                messageMentionDao.insert(
                                    MessageMention(
                                        mid,
                                        message.conversationId,
                                        mention.mentions,
                                        true
                                    )
                                )
                            }
                        }
                    } else {
                        messageDao.insert(
                            createPinMessage(
                                UUID.randomUUID().toString(),
                                data.conversationId,
                                data.userId,
                                messageId, // quote pinned message id
                                null,
                                nowInUtc(),
                                MessageStatus.READ.name
                            )
                        )
                        InvalidateFlow.emit(data.conversationId)
                    }
                    if (index == transferPinData.messageIds.size - 1) {
                        RxBus.publish(PinMessageEvent(data.conversationId, messageId))
                    }
                }
            } else if (transferPinData.action == PinAction.UNPIN.name) {
                pinMessageDao.deleteByIds(transferPinData.messageIds)
            }
            updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
            messageHistoryDao.insert(MessageHistory(data.messageId))
        }
    }

    private fun processRecallMessage(data: BlazeMessageData) {
        if (data.category == MessageCategory.MESSAGE_RECALL.name) {
            val decoded = Base64.decode(data.data)
            val transferRecallData = gson.fromJson(String(decoded), RecallMessagePayload::class.java)
            messageDao.findMessageById(transferRecallData.messageId)?.let { msg ->
                RxBus.publish(RecallEvent(msg.id))
                messageDao.recallFailedMessage(msg.id)
                messageDao.recallMessage(msg.id)
                messageDao.recallPinMessage(msg.id, msg.conversationId)
                pinMessageDao.deleteByMessageId(msg.id)
                messageMentionDao.deleteMessage(msg.id)
                messagesFts4Dao.deleteByMessageId(msg.id)
                remoteMessageStatusDao.deleteByMessageId(msg.id)
                remoteMessageStatusDao.updateConversationUnseen(msg.conversationId)
                if (msg.mediaUrl != null && mediaDownloaded(msg.mediaStatus)) {
                    msg.mediaUrl.getFilePath()?.let {
                        File(it).let { file ->
                            if (file.exists() && file.isFile) {
                                file.delete()
                            }
                        }
                    }
                }
                messageDao.findMessageItemById(data.conversationId, msg.id)?.let { quoteMsg ->
                    messageDao.updateQuoteContentByQuoteId(data.conversationId, msg.id, gson.toJson(quoteMsg))
                }

                jobManager.cancelJobByMixinJobId(msg.id)
                if (messageDao.findLastMessageId(msg.conversationId) == msg.id) {
                    notificationManager.cancel(msg.conversationId.hashCode())
                }
                InvalidateFlow.emit(msg.conversationId)
            }
            updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
            messageHistoryDao.insert(MessageHistory(data.messageId))
        }
    }

    private fun processPlainMessage(data: BlazeMessageData) {
        if (data.category == MessageCategory.PLAIN_JSON.name) {
            val json = Base64.decode(data.data)
            val plainData = gson.fromJson(String(json), PlainJsonMessagePayload::class.java)
            if (plainData.action == PlainDataAction.RESEND_KEY.name) {
                if (signalProtocol.containsUserSession(data.userId)) {
                    jobManager.addJobInBackground(SendProcessSignalKeyJob(data, ProcessSignalKeyAction.RESEND_KEY))
                }
            } else if (plainData.action == PlainDataAction.RESEND_MESSAGES.name) {
                processResendMessage(data, plainData)
            } else if (plainData.action == PlainDataAction.NO_KEY.name) {
                ratchetSenderKeyDao.delete(data.conversationId, SignalProtocolAddress(data.userId, data.sessionId.getDeviceId()).toString())
            } else if (plainData.action == PlainDataAction.ACKNOWLEDGE_MESSAGE_RECEIPTS.name) {
                plainData.ackMessages?.let {
                    val updateMessageList = arrayListOf<String>()
                    for (m in it) {
                        if (m.status != MessageStatus.READ.name && m.status != MessageMentionStatus.MENTION_READ.name) {
                            continue
                        }
                        if (m.status == MessageStatus.READ.name) {
                            updateMessageList.add(m.message_id)
                        } else if (m.status == MessageMentionStatus.MENTION_READ.name) {
                            messageMentionDao.markMentionRead(m.message_id)
                        }
                    }
                    if (updateMessageList.isNotEmpty()) {
                        remoteMessageStatusDao.deleteByMessageIds(updateMessageList)
                        val updateConversationList = messageDao.findConversationsByMessages(updateMessageList)
                        updateConversationList.forEach { cId ->
                            remoteMessageStatusDao.updateConversationUnseen(cId)
                            InvalidateFlow.emit(cId)
                            notificationManager.cancel(cId.hashCode())
                        }
                    }
                }
            }

            updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
            messageHistoryDao.insert(MessageHistory(data.messageId))
        } else if (data.category == MessageCategory.PLAIN_TEXT.name ||
            data.category == MessageCategory.PLAIN_IMAGE.name ||
            data.category == MessageCategory.PLAIN_VIDEO.name ||
            data.category == MessageCategory.PLAIN_DATA.name ||
            data.category == MessageCategory.PLAIN_AUDIO.name ||
            data.category == MessageCategory.PLAIN_STICKER.name ||
            data.category == MessageCategory.PLAIN_CONTACT.name ||
            data.category == MessageCategory.PLAIN_LIVE.name ||
            data.category == MessageCategory.PLAIN_POST.name ||
            data.category == MessageCategory.PLAIN_LOCATION.name ||
            data.category == MessageCategory.PLAIN_TRANSCRIPT.name
        ) {
            if (!data.representativeId.isNullOrBlank()) {
                data.userId = data.representativeId
            }
            try {
                processDecryptSuccess(data, data.data)
            } catch (e: Exception) {
                insertInvalidMessage(data)
            }
            updateRemoteMessageStatus(data.messageId)
        }
    }

    private fun processResendMessage(data: BlazeMessageData, plainData: PlainJsonMessagePayload) {
        val messages = plainData.messages ?: return
        val p = participantDao.findParticipantById(data.conversationId, data.userId) ?: return
        for (id in messages) {
            val resendMessage = resendMessageDao.findResendMessage(data.userId, data.sessionId, id)
            if (resendMessage != null) {
                continue
            }
            val accountId = Session.getAccountId() ?: return
            val needResendMessage = messageDao.findMessageById(id, accountId)
            if (needResendMessage == null || needResendMessage.category == MessageCategory.MESSAGE_RECALL.name) {
                resendMessageDao.insert(ResendSessionMessage(id, data.userId, data.sessionId, 0, nowInUtc()))
                continue
            }
            if (!needResendMessage.isSignal()) {
                continue
            }
            val pCreatedAt = ZonedDateTime.parse(p.createdAt)
            val mCreatedAt = ZonedDateTime.parse(needResendMessage.createdAt)
            if (pCreatedAt.isAfter(mCreatedAt)) {
                continue
            }
            needResendMessage.id = UUID.randomUUID().toString()
            jobManager.addJobInBackground(
                SendMessageJob(
                    needResendMessage,
                    ResendData(data.userId, id, data.sessionId),
                    true,
                    messagePriority = PRIORITY_SEND_ATTACHMENT_MESSAGE
                )
            )
            resendMessageDao.insert(ResendSessionMessage(id, data.userId, data.sessionId, 1, nowInUtc()))
        }
    }

    private fun generateMessage(
        data: BlazeMessageData,
        generator: (QuoteMessageItem?) -> Message
    ): Message {
        val quoteMessageId = data.quoteMessageId
        if (quoteMessageId.isNullOrBlank()) {
            return generator(null)
        }
        val quoteMessageItem =
            messageDao.findMessageItemById(data.conversationId, quoteMessageId)
                ?: return generator(null)
        return generator(quoteMessageItem)
    }

    private fun processDecryptSuccess(data: BlazeMessageData, plainText: String) {
        syncUser(data.userId)
        when {
            data.category.endsWith("_TEXT") -> {
                val plain = tryDecodePlain(data.category == MessageCategory.PLAIN_TEXT.name, plainText)
                var quoteMe = false
                val message = generateMessage(data) { quoteMessageItem ->
                    if (quoteMessageItem == null) {
                        createMessage(
                            data.messageId,
                            data.conversationId,
                            data.userId,
                            data.category,
                            plain,
                            data.createdAt,
                            data.status,
                            quoteMessageId = data.quoteMessageId
                        ).apply {
                            this.content?.findLastUrl()?.let {
                                this.hyperlink = it
                                parseHyperlink(data.messageId, it, hyperlinkDao, messageDao)
                            }
                        }
                    } else {
                        if (quoteMessageItem.userId == Session.getAccountId() && data.userId != Session.getAccountId()) {
                            quoteMe = true
                            messageMentionDao.insert(MessageMention(data.messageId, data.conversationId, "", false))
                        }
                        createReplyTextMessage(
                            data.messageId, data.conversationId, data.userId, data.category,
                            plain, data.createdAt, data.status, quoteMessageItem.messageId, quoteMessageItem.toJson()
                        )
                    }
                }
                val (mentions, mentionMe) = parseMentionData(plain, data.messageId, data.conversationId, userDao, messageMentionDao, data.userId)
                database.insertAndNotifyConversation(message)
                MessageFts4Helper.insertOrReplaceMessageFts4(message)
                val userMap = mentions?.map { it.identityNumber to it.fullName }?.toMap()
                generateNotification(message, data, userMap, quoteMe || mentionMe)
            }
            data.category.endsWith("_POST") -> {
                val plain = tryDecodePlain(data.category == MessageCategory.PLAIN_POST.name, plainText)
                val message = createPostMessage(
                    data.messageId,
                    data.conversationId,
                    data.userId,
                    data.category,
                    plain,
                    plain.postOptimize(),
                    data.createdAt,
                    data.status
                )
                database.insertAndNotifyConversation(message)
                MessageFts4Helper.insertOrReplaceMessageFts4(message)
                generateNotification(message, data)
            }
            data.category.endsWith("_LOCATION") -> {
                val plain = tryDecodePlain(data.category == MessageCategory.PLAIN_LOCATION.name, plainText)
                if (checkLocationData(plain)) {
                    val message = createLocationMessage(data.messageId, data.conversationId, data.userId, data.category, plain, data.status, data.createdAt)
                    database.insertAndNotifyConversation(message)
                    generateNotification(message, data)
                }
            }
            data.category.endsWith("_IMAGE") -> {
                val mediaData = gson.fromJson(
                    encryptedAttachmentContentDecode(data, plainText),
                    AttachmentMessagePayload::class.java
                )
                if (mediaData.invalidData()) {
                    insertInvalidMessage(data)
                    return
                }

                val message = generateMessage(data) { quoteMessageItem ->
                    createMediaMessage(
                        data.messageId, data.conversationId, data.userId, data.category, mediaData.attachmentId, null, mediaData.mimeType, mediaData.size,
                        mediaData.width, mediaData.height, mediaData.thumbnail, mediaData.key, mediaData.digest, data.createdAt, MediaStatus.CANCELED,
                        data.status, quoteMessageItem?.messageId, quoteMessageItem.toJson()
                    )
                }

                database.insertAndNotifyConversation(message)
                lifecycleScope.launch {
                    MixinApplication.appContext.autoDownload(autoDownloadPhoto) {
                        jobManager.addJobInBackground(AttachmentDownloadJob(message))
                    }
                }

                generateNotification(message, data)
            }
            data.category.endsWith("_VIDEO") -> {
                val mediaData = gson.fromJson(
                    encryptedAttachmentContentDecode(data, plainText),
                    AttachmentMessagePayload::class.java
                )
                if (mediaData.invalidData()) {
                    insertInvalidMessage(data)
                    return
                }

                val message = generateMessage(data) { quoteMessageItem ->
                    createVideoMessage(
                        data.messageId, data.conversationId, data.userId,
                        data.category, mediaData.attachmentId, mediaData.name, null, mediaData.duration,
                        mediaData.width, mediaData.height, mediaData.thumbnail, mediaData.mimeType,
                        mediaData.size, data.createdAt, mediaData.key, mediaData.digest, MediaStatus.CANCELED, data.status,
                        quoteMessageItem?.messageId, quoteMessageItem.toJson()
                    )
                }

                database.insertAndNotifyConversation(message)
                lifecycleScope.launch {
                    MixinApplication.appContext.autoDownload(autoDownloadVideo) {
                        jobManager.addJobInBackground(AttachmentDownloadJob(message))
                    }
                }
                generateNotification(message, data)
            }
            data.category.endsWith("_DATA") -> {
                val mediaData = gson.fromJson(
                    encryptedAttachmentContentDecode(data, plainText),
                    AttachmentMessagePayload::class.java
                )
                val message = generateMessage(data) { quoteMessageItem ->
                    createAttachmentMessage(
                        data.messageId, data.conversationId, data.userId,
                        data.category, mediaData.attachmentId, mediaData.name, null,
                        mediaData.mimeType, mediaData.size, data.createdAt,
                        mediaData.key, mediaData.digest, MediaStatus.CANCELED, data.status,
                        quoteMessageItem?.messageId, quoteMessageItem.toJson()
                    )
                }

                database.insertAndNotifyConversation(message)
                MessageFts4Helper.insertOrReplaceMessageFts4(message)
                lifecycleScope.launch {
                    MixinApplication.appContext.autoDownload(autoDownloadDocument) {
                        jobManager.addJobInBackground(AttachmentDownloadJob(message))
                    }
                }
                generateNotification(message, data)
            }
            data.category.endsWith("_AUDIO") -> {
                val mediaData = gson.fromJson(
                    encryptedAttachmentContentDecode(data, plainText),
                    AttachmentMessagePayload::class.java
                )
                val message = generateMessage(data) { quoteMessageItem ->
                    createAudioMessage(
                        data.messageId, data.conversationId, data.userId, mediaData.attachmentId,
                        data.category, mediaData.size, null, mediaData.duration.toString(), data.createdAt, mediaData.waveform,
                        mediaData.key, mediaData.digest, MediaStatus.PENDING, data.status,
                        quoteMessageItem?.messageId, quoteMessageItem.toJson()
                    )
                }
                database.insertAndNotifyConversation(message)
                jobManager.addJobInBackground(AttachmentDownloadJob(message))
                generateNotification(message, data)
            }
            data.category.endsWith("_STICKER") -> {
                val decoded = if (data.category.startsWith("ENCRYPTED_")) {
                    plainText
                } else {
                    String(Base64.decode(plainText))
                }
                val mediaData = gson.fromJson(decoded, StickerMessagePayload::class.java)
                val message = if (mediaData.stickerId == null) {
                    val sticker = stickerDao.getStickerByAlbumIdAndName(mediaData.albumId!!, mediaData.name!!)
                    if (sticker != null) {
                        createStickerMessage(
                            data.messageId, data.conversationId, data.userId, data.category, null,
                            mediaData.albumId, sticker.stickerId, mediaData.name, data.status, data.createdAt
                        )
                    } else {
                        return
                    }
                } else {
                    val sticker = stickerDao.getStickerByUnique(mediaData.stickerId)
                    if (sticker == null || (mediaData.albumId != null && sticker.albumId.isNullOrBlank())) {
                        jobManager.addJobInBackground(RefreshStickerJob(mediaData.stickerId))
                    }
                    createStickerMessage(
                        data.messageId, data.conversationId, data.userId, data.category, null,
                        mediaData.albumId, mediaData.stickerId, mediaData.name, data.status, data.createdAt
                    )
                }
                database.insertAndNotifyConversation(message)
                generateNotification(message, data)
            }
            data.category.endsWith("_CONTACT") -> {
                val decoded = if (data.category.startsWith("ENCRYPTED_")) {
                    plainText
                } else {
                    String(Base64.decode(plainText))
                }
                val contactData = gson.fromJson(decoded, ContactMessagePayload::class.java)
                val user = syncUser(contactData.userId)
                val message = generateMessage(data) { quoteMessageItem ->
                    createContactMessage(
                        data.messageId, data.conversationId, data.userId, data.category,
                        plainText, contactData.userId, data.status, data.createdAt, user?.fullName,
                        quoteMessageItem?.messageId, quoteMessageItem.toJson()
                    )
                }
                database.insertAndNotifyConversation(message)
                val fullName = user?.fullName
                if (!fullName.isNullOrBlank()) {
                    MessageFts4Helper.insertOrReplaceMessageFts4(message, fullName)
                }
                generateNotification(message, data)
            }
            data.category.endsWith("_LIVE") -> {
                val plain = if (data.category.startsWith("ENCRYPTED")) {
                    plainText
                } else {
                    String(Base64.decode(plainText))
                }
                val liveData = gson.fromJson(plain, LiveMessagePayload::class.java)
                if (liveData.width <= 0 || liveData.height <= 0) {
                    insertInvalidMessage(data)
                    return
                }
                val message = createLiveMessage(
                    data.messageId, data.conversationId, data.userId, data.category, plain,
                    liveData.width, liveData.height, liveData.url, liveData.thumbUrl, data.status, data.createdAt
                )
                database.insertAndNotifyConversation(message)
                generateNotification(message, data)
            }
            data.category.endsWith("_TRANSCRIPT") -> {
                val plain = if (data.category == MessageCategory.PLAIN_TRANSCRIPT.name) String(
                    Base64.decode(plainText)
                ) else plainText
                val message = processTranscriptMessage(data, plain) ?: return
                database.insertAndNotifyConversation(message)
                generateNotification(message, data)
            }
        }
    }

    private fun processTranscriptMessage(data: BlazeMessageData, plain: String): Message? {
        val transcripts =
            gson.fromJson(plain, Array<TranscriptMessage>::class.java).toList().filter { t ->
                t.transcriptId == data.messageId
            }
        if (transcripts.isEmpty()) {
            messageDao.insert(
                createTranscriptMessage(
                    data.messageId,
                    data.conversationId,
                    data.userId,
                    data.category,
                    null,
                    0,
                    data.createdAt,
                    MessageStatus.UNKNOWN.name
                )
            )
            InvalidateFlow.emit(data.conversationId)
            return null
        }
        val stringBuilder = StringBuilder()
        transcripts.filter { it.isText() || it.isPost() || it.isData() || it.isContact() }
            .forEach { transcript ->
                if (transcript.isData()) {
                    transcript.mediaName
                } else if (transcript.isContact()) {
                    transcript.sharedUserId?.let { userId -> userDao.findUser(userId) }?.fullName
                } else {
                    transcript.content
                }?.joinWhiteSpace()?.let {
                    stringBuilder.append(it)
                }
            }
        MessageFts4Helper.insertMessageFts4(data.messageId, stringBuilder.toString())

        transcripts.filter { t -> t.isSticker() || t.isContact() }.forEach { transcript ->
            transcript.stickerId?.let { stickerId ->
                val sticker = stickerDao.getStickerByUnique(stickerId)
                if (sticker == null) {
                    jobManager.addJobInBackground(RefreshStickerJob(stickerId))
                }
            }
            transcript.sharedUserId?.let { userId ->
                syncUser(userId)
            }
        }
        var mediaSize = 0L
        transcripts.filter { t -> t.isAttachment() }.forEach { transcript ->
            transcript.mediaStatus = MediaStatus.CANCELED.name
            transcript.mediaUrl = null
            transcript.mediaSize?.let {
                mediaSize += it
            }
            lifecycleScope.launch {
                when {
                    transcript.isImage() -> {
                        MixinApplication.appContext.autoDownload(autoDownloadPhoto) {
                            jobManager.addJobInBackground(
                                TranscriptAttachmentDownloadJob(
                                    data.conversationId,
                                    transcript
                                )
                            )
                        }
                    }
                    transcript.isVideo() -> {
                        MixinApplication.appContext.autoDownload(autoDownloadVideo) {
                            jobManager.addJobInBackground(
                                TranscriptAttachmentDownloadJob(
                                    data.conversationId,
                                    transcript
                                )
                            )
                        }
                    }
                    transcript.isData() -> {
                        MixinApplication.appContext.autoDownload(autoDownloadDocument) {
                            jobManager.addJobInBackground(
                                TranscriptAttachmentDownloadJob(
                                    data.conversationId,
                                    transcript
                                )
                            )
                        }
                    }
                    transcript.isAudio() -> {
                        jobManager.addJobInBackground(
                            TranscriptAttachmentDownloadJob(
                                data.conversationId,
                                transcript
                            )
                        )
                    }
                }
            }
        }
        val message = createTranscriptMessage(
            data.messageId,
            data.conversationId,
            data.userId,
            data.category,
            gson.toJson(
                transcripts.sortedBy { t -> t.createdAt }
                    .filter { t -> t.transcriptId == data.messageId }.map {
                        TranscriptMinimal(it.userFullName ?: "", it.type, it.content)
                    }
            ),
            mediaSize,
            data.createdAt,
            data.status
        )
        transcriptMessageDao.insertList(
            transcripts.filter { t ->
                transcriptMessageDao.getTranscriptByIdSync(t.transcriptId, t.messageId) == null
            }
        )
        if (!transcripts.any { t -> t.isAttachment() }) {
            message.mediaStatus = MediaStatus.DONE.name
        }
        return message
    }

    private fun processSystemSessionMessage(systemSession: SystemSessionMessagePayload) {
        if (systemSession.action == SystemSessionMessageAction.PROVISION.name) {
            Session.storeExtensionSessionId(systemSession.sessionId)
            signalProtocol.deleteSession(systemSession.userId)
            val conversations = conversationDao.getConversationsByUserId(systemSession.userId)
            val ps = conversations.filter {
                it.appId == null || it.capabilities?.contains(AppCap.ENCRYPTED.name) == true
            }.map {
                ParticipantSession(it.conversationId, systemSession.userId, systemSession.sessionId, publicKey = systemSession.publicKey)
            }
            if (ps.isNotEmpty()) {
                participantSessionDao.insertList(ps)
            }
        } else if (systemSession.action == SystemSessionMessageAction.DESTROY.name) {
            if (Session.getExtensionSessionId() != systemSession.sessionId) {
                return
            }
            Session.deleteExtensionSessionId()
            signalProtocol.deleteSession(systemSession.userId)
            participantSessionDao.deleteByUserIdAndSessionId(systemSession.userId, systemSession.sessionId)
        }
    }

    private fun processSystemSnapshotMessage(data: BlazeMessageData, snapshot: Snapshot) {
        val message = createMessage(
            data.messageId, data.conversationId, data.userId, data.category, "",
            data.createdAt, data.status, snapshot.type, null, snapshot.snapshotId
        )
        snapshot.transactionHash?.let {
            snapshotDao.deletePendingSnapshotByHash(it)
        }
        snapshotDao.insert(snapshot)
        database.insertAndNotifyConversation(message)
        jobManager.addJobInBackground(RefreshAssetsJob(snapshot.assetId))

        if (snapshot.type == SnapshotType.transfer.name && snapshot.amount.toFloat() > 0) {
            generateNotification(message, data)
        }
    }

    private fun processSystemConversationMessage(data: BlazeMessageData, systemMessage: SystemConversationMessagePayload) {
        var userId = data.userId
        if (systemMessage.userId != null) {
            userId = systemMessage.userId
        }
        if (userId == SYSTEM_USER && userDao.findUser(userId) == null) {
            userDao.insert(createSystemUser())
        }
        val message = createMessage(
            data.messageId, data.conversationId, userId, data.category, "",
            data.createdAt, data.status, systemMessage.action, systemMessage.participantId
        )

        val accountId = Session.getAccountId() ?: return
        if (systemMessage.action == SystemConversationAction.ADD.name || systemMessage.action == SystemConversationAction.JOIN.name) {
            participantDao.insert(Participant(data.conversationId, systemMessage.participantId!!, "", data.updatedAt))
            if (systemMessage.participantId == accountId) {
                jobManager.addJobInBackground(RefreshConversationJob(data.conversationId))
            } else {
                if (signalProtocol.isExistSenderKey(data.conversationId, accountId)) {
                    jobManager.addJobInBackground(SendProcessSignalKeyJob(data, ProcessSignalKeyAction.ADD_PARTICIPANT, systemMessage.participantId))
                } else {
                    jobManager.addJobInBackground(RefreshSessionJob(data.conversationId, arrayListOf(systemMessage.participantId)))
                }
                syncUser(systemMessage.participantId, data.conversationId)
            }
        } else if (systemMessage.action == SystemConversationAction.REMOVE.name || systemMessage.action == SystemConversationAction.EXIT.name) {
            if (systemMessage.participantId == accountId) {
                conversationDao.updateConversationStatusById(data.conversationId, ConversationStatus.QUIT.ordinal)
            }
            syncUser(systemMessage.participantId!!)
            jobManager.addJobInBackground(GenerateAvatarJob(data.conversationId))
            jobManager.addJobInBackground(SendProcessSignalKeyJob(data, ProcessSignalKeyAction.REMOVE_PARTICIPANT, systemMessage.participantId))
        } else if (systemMessage.action == SystemConversationAction.CREATE.name) {
        } else if (systemMessage.action == SystemConversationAction.UPDATE.name) {
            if (!systemMessage.participantId.isNullOrBlank()) {
                jobManager.addJobInBackground(RefreshUserJob(arrayListOf(systemMessage.participantId), forceRefresh = true))
            } else {
                jobManager.addJobInBackground(RefreshConversationJob(data.conversationId))
            }
            return
        } else if (systemMessage.action == SystemConversationAction.ROLE.name) {
            participantDao.updateParticipantRole(data.conversationId, systemMessage.participantId!!, systemMessage.role ?: "")
            if (message.participantId != accountId) {
                return
            }
        }
        database.insertAndNotifyConversation(message)
    }

    private fun processSystemUserMessage(systemMessage: SystemUserMessagePayload) {
        if (systemMessage.action == SystemUserMessageAction.UPDATE.name) {
            jobManager.addJobInBackground(RefreshUserJob(listOf(systemMessage.userId), forceRefresh = true))
        }
    }

    private fun processSystemCircleMessage(data: BlazeMessageData, systemMessage: SystemCircleMessagePayload) {
        when (systemMessage.action) {
            SystemCircleMessageAction.CREATE.name, SystemCircleMessageAction.UPDATE.name -> {
                jobManager.addJobInBackground(RefreshCircleJob(systemMessage.circleId))
            }
            SystemCircleMessageAction.ADD.name -> {
                val accountId = Session.getAccountId() ?: return
                val conversationId = systemMessage.conversationId ?: generateConversationId(accountId, systemMessage.userId ?: return)
                if (circleDao.findCircleById(systemMessage.circleId) == null) {
                    jobManager.addJobInBackground(RefreshCircleJob(systemMessage.circleId))
                }
                val circleConversation = CircleConversation(conversationId, systemMessage.circleId, systemMessage.userId, data.updatedAt, null)
                systemMessage.userId?.let { userId ->
                    syncUser(userId)
                }
                circleConversationDao.insertUpdate(circleConversation)
            }
            SystemCircleMessageAction.REMOVE.name -> {
                val accountId = Session.getAccountId() ?: return
                val conversationId = systemMessage.conversationId ?: generateConversationId(accountId, systemMessage.userId ?: return)
                circleConversationDao.deleteByIds(conversationId, systemMessage.circleId)
            }
            SystemCircleMessageAction.DELETE.name -> {
                runInTransaction {
                    circleDao.deleteCircleById(systemMessage.circleId)
                    circleConversationDao.deleteByCircleId(systemMessage.circleId)
                }
                RxBus.publish(CircleDeleteEvent(systemMessage.circleId))
                if (systemMessage.circleId == MixinApplication.appContext.defaultSharedPreferences.getString(Constants.CIRCLE.CIRCLE_ID, null)) {
                    MixinApplication.appContext.defaultSharedPreferences.putString(Constants.CIRCLE.CIRCLE_ID, null)
                }
            }
        }
    }

    private fun processEncryptedMessage(data: BlazeMessageData) {
        val privateKey = Session.getEd25519PrivateKey() ?: return
        val sessionId = Session.getSessionId() ?: return
        if (!data.representativeId.isNullOrBlank()) {
            data.userId = data.representativeId
        }
        try {
            val decryptedContent = encryptedProtocol.decryptMessage(privateKey, UUID.fromString(sessionId).toByteArray(), data.data.decodeBase64())
            val plaintext = String(decryptedContent)
            try {
                processDecryptSuccess(data, plaintext)
            } catch (e: JsonSyntaxException) {
                insertInvalidMessage(data)
            }
        } catch (e: Exception) {
            reportDecryptFailed(data, e, null)
            insertFailedMessage(data)
            updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
        }
    }

    private fun processSignalMessage(data: BlazeMessageData) {
        if (data.category == MessageCategory.SIGNAL_KEY.name) {
            updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
            messageHistoryDao.insert(MessageHistory(data.messageId))
        } else {
            updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
        }
        val deviceId = data.sessionId.getDeviceId()
        val (keyType, cipherText, resendMessageId) = SignalProtocol.decodeMessageData(data.data)
        try {
            signalProtocol.decrypt(
                data.conversationId,
                data.userId,
                keyType,
                cipherText,
                data.category,
                data.sessionId
            ) {
                if (data.category == MessageCategory.SIGNAL_KEY.name && data.userId != Session.getAccountId()) {
                    RxBus.publish(SenderKeyChange(data.conversationId, data.userId, data.sessionId))
                }
                if (data.category != MessageCategory.SIGNAL_KEY.name) {
                    val plaintext = String(it)
                    if (resendMessageId != null) {
                        processRedecryptMessage(data, resendMessageId, plaintext)
                        updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
                        messageHistoryDao.insert(MessageHistory(data.messageId))
                    } else {
                        try {
                            processDecryptSuccess(data, plaintext)
                        } catch (e: Exception) {
                            insertInvalidMessage(data)
                        }
                    }
                }
            }

            val address = SignalProtocolAddress(data.userId, deviceId)
            val status = ratchetSenderKeyDao.getRatchetSenderKey(data.conversationId, address.toString())?.status
            if (status == RatchetStatus.REQUESTING.name) {
                requestResendMessage(data.conversationId, data.userId, data.sessionId)
            }
        } catch (e: Exception) {
            Timber.e(e, "decrypt failed " + data.messageId)
            reportDecryptFailed(data, e, resendMessageId)

            if (resendMessageId != null) {
                return
            }
            refreshSignalKeys(data.conversationId)
            if (data.category == MessageCategory.SIGNAL_KEY.name) {
                ratchetSenderKeyDao.delete(data.conversationId, SignalProtocolAddress(data.userId, deviceId).toString())
            } else {
                insertFailedMessage(data)
                val address = SignalProtocolAddress(data.userId, deviceId)
                val status = ratchetSenderKeyDao.getRatchetSenderKey(data.conversationId, address.toString())?.status
                if (status == null) {
                    requestResendKey(data.conversationId, data.userId, data.messageId, data.sessionId)
                }
            }
        }
    }

    private fun reportDecryptFailed(data: BlazeMessageData, e: Exception, resendMessageId: String?) {
        FirebaseCrashlytics.getInstance().log("Decrypt failed$data$resendMessageId")
        FirebaseCrashlytics.getInstance().recordException(e)
        if (e !is NoSessionException) {
            reportException(
                """
                Decrypt failed
                BlazeMessageData: $data,
                resend_message: $resendMessageId
                """,
                e
            )
        }
    }

    private fun insertInvalidMessage(data: BlazeMessageData) {
        val message = createMessage(data.messageId, data.conversationId, data.userId, data.category, data.data, data.createdAt, MessageStatus.UNKNOWN.name)
        messageDao.insert(message)
        InvalidateFlow.emit(data.conversationId)
    }

    private fun insertFailedMessage(data: BlazeMessageData) {
        if (data.category == MessageCategory.SIGNAL_TEXT.name ||
            data.category == MessageCategory.SIGNAL_IMAGE.name ||
            data.category == MessageCategory.SIGNAL_VIDEO.name ||
            data.category == MessageCategory.SIGNAL_DATA.name ||
            data.category == MessageCategory.SIGNAL_AUDIO.name ||
            data.category == MessageCategory.SIGNAL_STICKER.name ||
            data.category == MessageCategory.SIGNAL_CONTACT.name ||
            data.category == MessageCategory.SIGNAL_LOCATION.name ||
            data.category == MessageCategory.SIGNAL_POST.name ||
            data.category == MessageCategory.SIGNAL_TRANSCRIPT.name ||
            data.category == MessageCategory.ENCRYPTED_TEXT.name ||
            data.category == MessageCategory.ENCRYPTED_IMAGE.name ||
            data.category == MessageCategory.ENCRYPTED_VIDEO.name ||
            data.category == MessageCategory.ENCRYPTED_STICKER.name ||
            data.category == MessageCategory.ENCRYPTED_DATA.name ||
            data.category == MessageCategory.ENCRYPTED_CONTACT.name ||
            data.category == MessageCategory.ENCRYPTED_AUDIO.name ||
            data.category == MessageCategory.ENCRYPTED_LIVE.name ||
            data.category == MessageCategory.ENCRYPTED_POST.name ||
            data.category == MessageCategory.ENCRYPTED_LOCATION.name ||
            data.category == MessageCategory.ENCRYPTED_TRANSCRIPT.name
        ) {
            database.insertAndNotifyConversation(
                createMessage(
                    data.messageId,
                    data.conversationId,
                    data.userId,
                    data.category,
                    data.data,
                    data.createdAt,
                    MessageStatus.FAILED.name
                )
            )
        }
    }

    private fun processRedecryptMessage(data: BlazeMessageData, messageId: String, plainText: String) {
        if (data.category == MessageCategory.SIGNAL_TEXT.name) {
            parseMentionData(plainText, messageId, data.conversationId, userDao, messageMentionDao, data.userId)
            messageDao.updateMessageContentAndStatus(plainText, data.status, messageId)
            InvalidateFlow.emit(data.conversationId)
        } else if (data.category == MessageCategory.SIGNAL_POST.name) {
            messageDao.updateMessageContentAndStatus(plainText, data.status, messageId)
            InvalidateFlow.emit(data.conversationId)
        } else if (data.category == MessageCategory.SIGNAL_LOCATION.name) {
            if (checkLocationData(plainText)) {
                messageDao.updateMessageContentAndStatus(plainText, data.status, messageId)
                InvalidateFlow.emit(data.conversationId)
            }
        } else if (data.category == MessageCategory.SIGNAL_IMAGE.name ||
            data.category == MessageCategory.SIGNAL_VIDEO.name ||
            data.category == MessageCategory.SIGNAL_AUDIO.name ||
            data.category == MessageCategory.SIGNAL_DATA.name
        ) {
            val decoded = Base64.decode(plainText)
            val mediaData = gson.fromJson(String(decoded), AttachmentMessagePayload::class.java)
            val duration = mediaData.duration?.toString()
            messageDao.updateAttachmentMessage(
                messageId, mediaData.attachmentId, mediaData.mimeType, mediaData.size,
                mediaData.width, mediaData.height, mediaData.thumbnail, mediaData.name, mediaData.waveform, duration,
                mediaData.key, mediaData.digest, MediaStatus.CANCELED.name, data.status
            )
            InvalidateFlow.emit(data.conversationId)
            if (data.category == MessageCategory.SIGNAL_IMAGE.name || data.category == MessageCategory.SIGNAL_AUDIO.name) {
                val message = messageDao.findMessageById(messageId)!!
                jobManager.addJobInBackground(AttachmentDownloadJob(message))
            }
        } else if (data.category == MessageCategory.SIGNAL_STICKER.name) {
            val decoded = Base64.decode(plainText)
            val stickerData = gson.fromJson(String(decoded), StickerMessagePayload::class.java)
            if (stickerData.stickerId != null) {
                val sticker = stickerDao.getStickerByUnique(stickerData.stickerId)
                if (sticker == null || (stickerData.albumId != null && sticker.albumId.isNullOrBlank())) {
                    jobManager.addJobInBackground(RefreshStickerJob(stickerData.stickerId))
                }
            }
            stickerData.stickerId?.let { messageDao.updateStickerMessage(it, data.status, messageId) }
            InvalidateFlow.emit(data.conversationId)
        } else if (data.category == MessageCategory.SIGNAL_CONTACT.name) {
            val decoded = Base64.decode(plainText)
            val contactData = gson.fromJson(String(decoded), ContactMessagePayload::class.java)
            messageDao.updateContactMessage(contactData.userId, data.status, messageId)
            syncUser(contactData.userId)
            InvalidateFlow.emit(data.conversationId)
        } else if (data.category == MessageCategory.SIGNAL_LIVE.name) {
            val decoded = Base64.decode(plainText)
            val liveData = gson.fromJson(String(decoded), LiveMessagePayload::class.java)
            messageDao.updateLiveMessage(liveData.width, liveData.height, liveData.url, liveData.thumbUrl, data.status, messageId)
            InvalidateFlow.emit(data.conversationId)
        } else if (data.category == MessageCategory.SIGNAL_TRANSCRIPT.name) {
            val decoded = Base64.decode(plainText)
            processTranscriptMessage(data, String(decoded))?.let { message ->
                messageDao.updateTranscriptMessage(message.content, message.mediaSize, message.mediaStatus, message.status, messageId)
                InvalidateFlow.emit(data.conversationId)
            }
        }
        if (messageDao.countMessageByQuoteId(data.conversationId, messageId) > 0) {
            messageDao.findMessageItemById(data.conversationId, messageId)?.let {
                messageDao.updateQuoteContentByQuoteId(data.conversationId, messageId, gson.toJson(it))
                InvalidateFlow.emit(data.conversationId)
            }
        }
    }

    private fun requestResendKey(conversationId: String, recipientId: String, messageId: String, sessionId: String?) {
        val plainText = gson.toJson(
            PlainJsonMessagePayload(
                action = PlainDataAction.RESEND_KEY.name,
                messageId = messageId
            )
        )
        val encoded = plainText.toByteArray().base64Encode()
        val bm = createParamBlazeMessage(createPlainJsonParam(conversationId, recipientId, encoded, sessionId))
        jobManager.addJobInBackground(SendPlaintextJob(bm))

        val address = SignalProtocolAddress(recipientId, sessionId.getDeviceId())
        val ratchet = RatchetSenderKey(conversationId, address.toString(), RatchetStatus.REQUESTING.name, bm.params?.message_id, nowInUtc())
        ratchetSenderKeyDao.insert(ratchet)
    }

    private fun requestResendMessage(conversationId: String, userId: String, sessionId: String?) {
        val messages = messageDao.findFailedMessages(conversationId, userId)
        if (messages.isEmpty()) {
            return
        }
        val plainText = gson.toJson(PlainJsonMessagePayload(PlainDataAction.RESEND_MESSAGES.name, messages.reversed()))
        val bm = createParamBlazeMessage(createPlainJsonParam(conversationId, userId, plainText.base64Encode(), sessionId))
        jobManager.addJobInBackground(SendPlaintextJob(bm))
        ratchetSenderKeyDao.delete(conversationId, SignalProtocolAddress(userId, sessionId.getDeviceId()).toString())
    }

    private fun updateRemoteMessageStatus(messageId: String, status: MessageStatus = MessageStatus.DELIVERED) {
        jobDao.insertNoReplace(createAckJob(ACKNOWLEDGE_MESSAGE_RECEIPTS, BlazeAckMessage(messageId, status.name)))
    }

    private fun refreshSignalKeys(conversationId: String) {
        val start = refreshKeyMap[conversationId] ?: 0.toLong()
        val current = System.currentTimeMillis()
        if (start == 0.toLong()) {
            refreshKeyMap[conversationId] = current
        }
        if (current - start < 1000 * 60) {
            return
        }
        refreshKeyMap[conversationId] = current
        val blazeMessage = createCountSignalKeys()
        val data = signalKeysChannel(blazeMessage) ?: return
        val count = gson.fromJson(data, SignalKeyCount::class.java)
        if (count.preKeyCount >= RefreshOneTimePreKeysJob.PREKEY_MINI_NUM) {
            return
        }

        val bm = createSyncSignalKeys(createSyncSignalKeysParam(RefreshOneTimePreKeysJob.generateKeys()))
        val result = signalKeysChannel(bm)
        if (result == null) {
            Timber.tag(TAG).w("Registering new pre keys...")
        }
    }

    private fun encryptedAttachmentContentDecode(
        data: BlazeMessageData,
        plainText: String
    ): String {
        return if (data.category.startsWith("ENCRYPTED")) {
            plainText
        } else {
            String(Base64.decode(plainText))
        }
    }

    private fun tryDecodePlain(isPlain: Boolean, plainText: String) =
        if (isPlain) {
            try {
                String(Base64.decode(plainText))
            } catch (e: IOException) {
                plainText
            }
        } else plainText

    private fun generateNotification(message: Message, data: BlazeMessageData, userMap: Map<String, String>? = null, force: Boolean = false) {
        if (data.source == LIST_PENDING_MESSAGES) {
            return
        }
        if (MixinApplication.conversationId == message.conversationId) {
            return
        }
        if (message.userId == Session.getAccountId()) {
            return
        }
        NotificationGenerator.generate(lifecycleScope, message, userMap, force, data.silent ?: false)
    }
}
