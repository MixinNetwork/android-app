package one.mixin.android.job

import android.util.Log
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bugsnag.android.Bugsnag
import com.google.gson.Gson
import one.mixin.android.MixinApplication
import one.mixin.android.MixinApplication.Companion.conversationId
import one.mixin.android.api.response.SignalKeyCount
import one.mixin.android.crypto.Base64
import one.mixin.android.crypto.SignalProtocol
import one.mixin.android.crypto.SignalProtocol.Companion.DEFAULT_DEVICE_ID
import one.mixin.android.crypto.vo.RatchetSenderKey
import one.mixin.android.crypto.vo.RatchetStatus
import one.mixin.android.extension.arrayMapOf
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.extension.findLastUrl
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.BaseJob.Companion.PRIORITY_SEND_ATTACHMENT_MESSAGE
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.Session
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageHistory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.ResendMessage
import one.mixin.android.vo.SYSTEM_USER
import one.mixin.android.vo.Snapshot
import one.mixin.android.vo.SnapshotType
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.createAttachmentMessage
import one.mixin.android.vo.createAudioMessage
import one.mixin.android.vo.createContactMessage
import one.mixin.android.vo.createMediaMessage
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.createReplyMessage
import one.mixin.android.vo.createStickerMessage
import one.mixin.android.vo.createSystemUser
import one.mixin.android.vo.createVideoMessage
import one.mixin.android.vo.isIllegalMessageCategory
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessageData
import one.mixin.android.websocket.LIST_PENDING_MESSAGES
import one.mixin.android.websocket.PlainDataAction
import one.mixin.android.websocket.ResendData
import one.mixin.android.websocket.SystemConversationAction
import one.mixin.android.websocket.SystemConversationData
import one.mixin.android.websocket.TransferAttachmentData
import one.mixin.android.websocket.TransferContactData
import one.mixin.android.websocket.TransferPlainData
import one.mixin.android.websocket.TransferStickerData
import one.mixin.android.websocket.createCountSignalKeys
import one.mixin.android.websocket.createParamBlazeMessage
import one.mixin.android.websocket.createPlainJsonParam
import one.mixin.android.websocket.createSyncSignalKeys
import one.mixin.android.websocket.createSyncSignalKeysParam
import one.mixin.android.websocket.invalidData
import one.mixin.android.work.RefreshAssetsWorker
import one.mixin.android.work.RefreshConversationWorker
import one.mixin.android.work.RefreshStickerWorker
import one.mixin.android.work.RefreshUserWorker
import org.whispersystems.libsignal.DecryptionCallback
import org.whispersystems.libsignal.NoSessionException
import org.whispersystems.libsignal.SignalProtocolAddress
import timber.log.Timber
import java.io.IOException
import java.util.UUID

class DecryptMessage : Injector() {

    companion object {
        val TAG = DecryptMessage::class.java.simpleName
        const val GROUP = "DecryptMessage"
    }

    private var refreshKeyMap = arrayMapOf<String, Long?>()
    private val gson = Gson()
    private val customGson = GsonHelper.customGson
    private var handled = false

    fun onRun(data: BlazeMessageData) {
        if (!isExistMessage(data.messageId)) {
            processMessage(data)
        }
    }

    private fun processMessage(data: BlazeMessageData) {
        try {
            if (data.category.isIllegalMessageCategory()) {
                updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
                return
            }

            syncConversation(data)
            processSystemMessage(data)
            processPlainMessage(data)
            processSignalMessage(data)
            processAppButton(data)
            processAppCard(data)
        } catch (e: Exception) {
            Timber.e("Process error: $e")
            updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
        }
    }

    private fun processAppButton(data: BlazeMessageData) {
        if (data.category != MessageCategory.APP_BUTTON_GROUP.name) {
            return
        }
        val message = createMessage(data.messageId, data.conversationId, data.userId, data.category,
            String(Base64.decode(data.data)), data.createdAt, MessageStatus.DELIVERED)
        messageDao.insert(message)
        updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
        handled = true
    }

    private fun processAppCard(data: BlazeMessageData) {
        if (data.category != MessageCategory.APP_CARD.name) {
            return
        }
        val message = createMessage(data.messageId, data.conversationId, data.userId, data.category,
            String(Base64.decode(data.data)), data.createdAt, MessageStatus.DELIVERED)
        messageDao.insert(message)
        updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
        handled = true
    }

    private fun processSystemMessage(data: BlazeMessageData) {
        if (!data.category.startsWith("SYSTEM_")) {
            return
        }
        if (data.category == MessageCategory.SYSTEM_CONVERSATION.name) {
            val json = Base64.decode(data.data)
            val systemMessage = gson.fromJson(String(json), SystemConversationData::class.java)
            processSystemConversationMessage(data, systemMessage)
        } else if (data.category == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name) {
            val json = Base64.decode(data.data)
            val systemSnapshot = gson.fromJson(String(json), Snapshot::class.java)
            processSystemSnapshotMessage(data, systemSnapshot)
        }

        updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
        handled = true
    }

    private fun processPlainMessage(data: BlazeMessageData) {
        if (!data.category.startsWith("PLAIN_")) {
            return
        }
        if (data.category == MessageCategory.PLAIN_JSON.name) {
            val json = Base64.decode(data.data)
            val plainData = gson.fromJson(String(json), TransferPlainData::class.java)
            if (plainData.action == PlainDataAction.RESEND_KEY.name) {
                if (signalProtocol.containsSession(data.userId)) {
                    jobManager.addJobInBackground(SendProcessSignalKeyJob(data, ProcessSignalKeyAction.RESEND_KEY))
                }
            } else if (plainData.action == PlainDataAction.RESEND_MESSAGES.name) {
                for (mId in plainData.messages!!) {
                    val resendMessage = resendMessageDao.findResendMessage(data.userId, mId)
                    if (resendMessage != null) {
                        continue
                    }
                    val needResendMessage = messageDao.findMessageById(mId)
                    if (needResendMessage != null) {
                        needResendMessage.id = UUID.randomUUID().toString()
                        jobManager.addJobInBackground(SendMessageJob(needResendMessage,
                            ResendData(data.userId, mId), true, messagePriority = PRIORITY_SEND_ATTACHMENT_MESSAGE))
                        resendMessageDao.insert(ResendMessage(mId, data.userId, 1, nowInUtc()))
                    } else {
                        resendMessageDao.insert(ResendMessage(mId, data.userId, 0, nowInUtc()))
                    }
                }
            } else if (plainData.action == PlainDataAction.NO_KEY.name) {
                ratchetSenderKeyDao.delete(data.conversationId, SignalProtocolAddress(data.userId,
                    DEFAULT_DEVICE_ID).toString())
            }

            updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
            messageHistoryDao.insert(MessageHistory(data.messageId))
        } else if (data.category == MessageCategory.PLAIN_TEXT.name ||
            data.category == MessageCategory.PLAIN_IMAGE.name ||
            data.category == MessageCategory.PLAIN_VIDEO.name ||
            data.category == MessageCategory.PLAIN_DATA.name ||
            data.category == MessageCategory.PLAIN_AUDIO.name ||
            data.category == MessageCategory.PLAIN_STICKER.name ||
            data.category == MessageCategory.PLAIN_CONTACT.name) {
            if (!data.representativeId.isNullOrBlank()) {
                data.userId = data.representativeId
            }
            processDecryptSuccess(data, data.data)
            updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
        }
        handled = true
    }

    private fun processDecryptSuccess(data: BlazeMessageData, plainText: String) {
        syncUser(data.userId)
        when {
            data.category.endsWith("_TEXT") -> {
                val plain = if (data.category == MessageCategory.PLAIN_TEXT.name) String(Base64.decode(plainText)) else plainText
                val message = if (data.quoteMessageId == null) {
                    createMessage(data.messageId, data.conversationId, data.userId, data.category,
                        plain, data.createdAt, MessageStatus.DELIVERED)
                        .apply {
                            this.content?.findLastUrl()?.let { jobManager.addJobInBackground(ParseHyperlinkJob(it, data.messageId)) }
                        }
                } else {
                    val quoteMsg = messageDao.findMessageItemById(data.conversationId, data.quoteMessageId)
                    if (quoteMsg != null) {
                        createReplyMessage(data.messageId, data.conversationId, data.userId, data.category,
                            plain, data.createdAt, MessageStatus.DELIVERED, data.quoteMessageId, gson.toJson(quoteMsg))
                    } else {
                        createReplyMessage(data.messageId, data.conversationId, data.userId, data.category,
                            plain, data.createdAt, MessageStatus.DELIVERED, data.quoteMessageId)
                    }
                }

                messageDao.insert(message)
                sendNotificationJob(message, data.source)
            }
            data.category.endsWith("_IMAGE") -> {
                val decoded = Base64.decode(plainText)
                val mediaData = customGson.fromJson(String(decoded), TransferAttachmentData::class.java)
                if (mediaData.invalidData()) {
                    return
                }
                val mimeType = if (mediaData.mimeType.isNullOrEmpty()) mediaData.mineType else mediaData.mimeType
                val message = createMediaMessage(data.messageId, data.conversationId, data.userId, data.category,
                    mediaData.attachmentId, null,
                    mimeType, mediaData.size, mediaData.width, mediaData.height, mediaData.thumbnail,
                    mediaData.key, mediaData.digest, data.createdAt, MediaStatus.PENDING, MessageStatus.DELIVERED)

                messageDao.insert(message)
                jobManager.addJobInBackground(AttachmentDownloadJob(message))
                sendNotificationJob(message, data.source)
            }
            data.category.endsWith("_VIDEO") -> {
                val decoded = Base64.decode(plainText)
                val mediaData = customGson.fromJson(String(decoded), TransferAttachmentData::class.java)
                if (mediaData.invalidData()) {
                    return
                }
                val mimeType = if (mediaData.mimeType.isEmpty()) mediaData.mineType else mediaData.mimeType
                val message = createVideoMessage(data.messageId, data.conversationId, data.userId,
                    data.category, mediaData.attachmentId, mediaData.name, null, mediaData.duration,
                    mediaData.width, mediaData.height, mediaData.thumbnail, mimeType,
                    mediaData.size, data.createdAt, mediaData.key, mediaData.digest, MediaStatus.CANCELED, MessageStatus.DELIVERED)
                messageDao.insert(message)
                sendNotificationJob(message, data.source)
            }
            data.category.endsWith("_DATA") -> {
                val decoded = Base64.decode(plainText)
                val mediaData = customGson.fromJson(String(decoded), TransferAttachmentData::class.java)
                val mimeType = if (mediaData.mimeType.isEmpty()) mediaData.mineType else mediaData.mimeType
                val message = createAttachmentMessage(data.messageId, data.conversationId, data.userId,
                    data.category, mediaData.attachmentId, mediaData.name, null,
                    mimeType, mediaData.size, data.createdAt,
                    mediaData.key, mediaData.digest, MediaStatus.CANCELED, MessageStatus.DELIVERED)
                messageDao.insert(message)
                sendNotificationJob(message, data.source)
            }
            data.category.endsWith("_AUDIO") -> {
                val decoded = Base64.decode(plainText)
                val mediaData = customGson.fromJson(String(decoded), TransferAttachmentData::class.java)
                val message = createAudioMessage(data.messageId, data.conversationId, data.userId, mediaData.attachmentId,
                    data.category, mediaData.size, null, mediaData.duration.toString(), nowInUtc(), mediaData.waveform,
                    mediaData.key, mediaData.digest, MediaStatus.PENDING, MessageStatus.DELIVERED)
                messageDao.insert(message)
                jobManager.addJobInBackground(AttachmentDownloadJob(message))
                sendNotificationJob(message, data.source)
            }
            data.category.endsWith("_STICKER") -> {
                val decoded = Base64.decode(plainText)
                val mediaData = customGson.fromJson(String(decoded), TransferStickerData::class.java)
                val message = if (mediaData.stickerId == null) {
                    val sticker = stickerDao.getStickerByAlbumIdAndName(mediaData.albumId!!, mediaData.name!!)
                    if (sticker != null) {
                        createStickerMessage(data.messageId, data.conversationId, data.userId, data.category, null,
                            mediaData.albumId, sticker.stickerId, mediaData.name, MessageStatus.DELIVERED, data.createdAt)
                    } else {
                        return
                    }
                } else {
                    val sticker = stickerDao.getStickerByUnique(mediaData.stickerId)
                    if (sticker == null) {
                        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshStickerWorker>(
                            workDataOf(RefreshStickerWorker.STICKER_ID to mediaData.stickerId))
                    }
                    createStickerMessage(data.messageId, data.conversationId, data.userId, data.category, null,
                        mediaData.albumId, mediaData.stickerId, mediaData.name, MessageStatus.DELIVERED, data.createdAt)
                }
                messageDao.insert(message)
                sendNotificationJob(message, data.source)
            }
            data.category.endsWith("_CONTACT") -> {
                val decoded = Base64.decode(plainText)
                val contactData = customGson.fromJson(String(decoded), TransferContactData::class.java)
                val message = createContactMessage(data.messageId, data.conversationId, data.userId, data.category,
                    plainText, contactData.userId, MessageStatus.DELIVERED, data.createdAt)
                messageDao.insert(message)
                syncUser(contactData.userId)
                sendNotificationJob(message, data.source)
            }
        }
    }

    private fun processSystemSnapshotMessage(data: BlazeMessageData, snapshot: Snapshot) {
        val message = createMessage(data.messageId, data.conversationId, data.userId, data.category, "",
            data.createdAt, MessageStatus.DELIVERED, snapshot.type, null, snapshot.snapshotId)
        snapshot.transactionHash?.let {
            snapshotDao.deletePendingSnapshotByHash(it)
        }
        snapshotDao.insert(snapshot)
        messageDao.insert(message)
        if (assetDao.simpleAsset(snapshot.assetId) == null) {
            WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshAssetsWorker>(
                workDataOf(RefreshAssetsWorker.ASSET_ID to snapshot.assetId))
        }
        if (snapshot.type == SnapshotType.transfer.name && snapshot.amount.toFloat() > 0) {
            sendNotificationJob(message, data.source)
        }
    }

    private fun processSystemConversationMessage(data: BlazeMessageData, systemMessage: SystemConversationData) {
        var userId = data.userId
        if (systemMessage.userId != null) {
            userId = systemMessage.userId
        }
        if (userId == SYSTEM_USER) {
            userDao.insert(createSystemUser())
        }
        val message = createMessage(data.messageId, data.conversationId, userId, data.category, "",
            data.createdAt, MessageStatus.DELIVERED, systemMessage.action, systemMessage.participantId)

        val accountId = Session.getAccountId()
        if (systemMessage.action == SystemConversationAction.ADD.name ||
            systemMessage.action == SystemConversationAction.JOIN.name) {
            participantDao.insert(Participant(data.conversationId, systemMessage.participantId!!, "", data.updatedAt))
            if (systemMessage.participantId == accountId) {
                WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshConversationWorker>(
                    workDataOf(RefreshConversationWorker.CONVERSATION_ID to data.conversationId))
            } else {
                WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshUserWorker>(
                    workDataOf(RefreshUserWorker.USER_IDS to arrayOf(systemMessage.participantId),
                        RefreshUserWorker.CONVERSATION_ID to data.conversationId))
            }
            if (systemMessage.participantId != accountId &&
                signalProtocol.isExistSenderKey(data.conversationId, accountId!!)) {
                jobManager.addJobInBackground(SendProcessSignalKeyJob(data,
                    ProcessSignalKeyAction.ADD_PARTICIPANT, systemMessage.participantId))
            }
        } else if (systemMessage.action == SystemConversationAction.REMOVE.name ||
            systemMessage.action == SystemConversationAction.EXIT.name) {

            if (systemMessage.participantId == accountId) {
                conversationDao.updateConversationStatusById(data.conversationId, ConversationStatus.QUIT.ordinal)
            } else {
                jobManager.addJobInBackground(GenerateAvatarJob(data.conversationId))
            }
            syncUser(systemMessage.participantId!!)
            jobManager.addJobInBackground(SendProcessSignalKeyJob(data, ProcessSignalKeyAction.REMOVE_PARTICIPANT, systemMessage.participantId))
        } else if (systemMessage.action == SystemConversationAction.CREATE.name) {
        } else if (systemMessage.action == SystemConversationAction.UPDATE.name) {
            WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshConversationWorker>(
                workDataOf(RefreshConversationWorker.CONVERSATION_ID to data.conversationId))
            return
        } else if (systemMessage.action == SystemConversationAction.ROLE.name) {
            participantDao.updateParticipantRole(data.conversationId,
                systemMessage.participantId!!, systemMessage.role!!)
            if (message.participantId != accountId) {
                return
            }
        }
        messageDao.insert(message)
    }

    private fun processSignalMessage(data: BlazeMessageData) {
        if (!data.category.startsWith("SIGNAL_")) {
            return
        }

        if (data.category == MessageCategory.SIGNAL_KEY.name) {
            updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
            messageHistoryDao.insert(MessageHistory(data.messageId))
        } else {
            updateRemoteMessageStatus(data.messageId, MessageStatus.DELIVERED)
        }

        val (keyType, cipherText, resendMessageId) = SignalProtocol.decodeMessageData(data.data)
        try {
            signalProtocol.decrypt(data.conversationId, data.userId, keyType, cipherText, data.category, DecryptionCallback {
                if (data.category != MessageCategory.SIGNAL_KEY.name) {
                    val plaintext = String(it)
                    if (resendMessageId != null) {
                        processRedecryptMessage(data, resendMessageId, plaintext)
                        updateRemoteMessageStatus(data.messageId, MessageStatus.READ)
                        messageHistoryDao.insert(MessageHistory(data.messageId))
                    } else {
                        processDecryptSuccess(data, plaintext)
                    }
                }
            })

            val address = SignalProtocolAddress(data.userId, DEFAULT_DEVICE_ID)
            val status = ratchetSenderKeyDao.getRatchetSenderKey(data.conversationId, address.toString())?.status
            if (status != null) {
                if (status == RatchetStatus.REQUESTING.name) {
                    requestResendMessage(data.conversationId, data.userId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "decrypt failed " + data.messageId, e)
            if (e !is NoSessionException) {
                Bugsnag.beforeNotify {
                    it.addToTab("Decrypt", "conversation", data.conversationId)
                    it.addToTab("Decrypt", "message_id", data.messageId)
                    it.addToTab("Decrypt", "user", data.userId)
                    it.addToTab("Decrypt", "data", data.data)
                    it.addToTab("Decrypt", "category", data.category)
                    it.addToTab("Decrypt", "created_at", data.createdAt)
                    it.addToTab("Decrypt", "resend_message", resendMessageId ?: "")
                    true
                }
                Bugsnag.notify(e)
            }

            if (resendMessageId != null) {
                return
            }
            if (data.category == MessageCategory.SIGNAL_KEY.name) {
                ratchetSenderKeyDao.delete(data.conversationId, SignalProtocolAddress(data.userId,
                    DEFAULT_DEVICE_ID).toString())
                refreshKeys(data.conversationId)
            } else {
                insertFailedMessage(data)
                refreshKeys(data.conversationId)
                val address = SignalProtocolAddress(data.userId, DEFAULT_DEVICE_ID)
                val status = ratchetSenderKeyDao.getRatchetSenderKey(data.conversationId, address.toString())?.status
                if (status == null || (status != RatchetStatus.REQUESTING.name &&
                        status != RatchetStatus.REQUESTING_MESSAGE.name)) {
                    requestResendKey(data.conversationId, data.userId, data.messageId)
                }
            }
        }
        handled = true
    }

    private fun insertFailedMessage(data: BlazeMessageData) {
        if (data.category == MessageCategory.SIGNAL_TEXT.name ||
            data.category == MessageCategory.SIGNAL_IMAGE.name ||
            data.category == MessageCategory.SIGNAL_VIDEO.name ||
            data.category == MessageCategory.SIGNAL_DATA.name ||
            data.category == MessageCategory.SIGNAL_AUDIO.name ||
            data.category == MessageCategory.SIGNAL_STICKER.name ||
            data.category == MessageCategory.SIGNAL_CONTACT.name) {
            messageDao.insert(createMessage(data.messageId, data.conversationId,
                data.userId, data.category, data.data, data.createdAt, MessageStatus.FAILED))
        }
    }

    private fun processRedecryptMessage(data: BlazeMessageData, messageId: String, plainText: String) {
        if (data.category == MessageCategory.SIGNAL_TEXT.name) {
            messageDao.updateMessageContentAndStatus(plainText, MessageStatus.DELIVERED.name, messageId)
        } else if (data.category == MessageCategory.SIGNAL_IMAGE.name ||
            data.category == MessageCategory.SIGNAL_VIDEO.name ||
            data.category == MessageCategory.SIGNAL_AUDIO.name ||
            data.category == MessageCategory.SIGNAL_DATA.name) {
            val decoded = Base64.decode(plainText)
            val mediaData = customGson.fromJson(String(decoded), TransferAttachmentData::class.java)
            val duration = if (mediaData.duration == null) null else mediaData.duration.toString()
            val mimeType = if (mediaData.mimeType.isEmpty()) mediaData.mineType else mediaData.mimeType
            messageDao.updateAttachmentMessage(messageId, mediaData.attachmentId, mimeType, mediaData.size,
                mediaData.width, mediaData.height, mediaData.thumbnail, mediaData.name, mediaData.waveform, duration,
                mediaData.key, mediaData.digest, MediaStatus.CANCELED.name, MessageStatus.DELIVERED.name)
            if (data.category == MessageCategory.SIGNAL_IMAGE.name || data.category == MessageCategory.SIGNAL_AUDIO.name) {
                val message = messageDao.findMessageById(messageId)!!
                jobManager.addJobInBackground(AttachmentDownloadJob(message))
            }
        } else if (data.category == MessageCategory.SIGNAL_STICKER.name) {
            val decoded = Base64.decode(plainText)
            val stickerData = customGson.fromJson(String(decoded), TransferStickerData::class.java)
            if (stickerData.stickerId != null) {
                val sticker = stickerDao.getStickerByUnique(stickerData.stickerId)
                if (sticker == null) {
                    WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshStickerWorker>(
                        workDataOf(RefreshStickerWorker.STICKER_ID to stickerData.stickerId))
                }
            }
            stickerData.stickerId?.let { messageDao.updateStickerMessage(it, MessageStatus.DELIVERED.name, messageId) }
        } else if (data.category == MessageCategory.SIGNAL_CONTACT.name) {
            val decoded = Base64.decode(plainText)
            val contactData = customGson.fromJson(String(decoded), TransferContactData::class.java)
            messageDao.updateContactMessage(contactData.userId, MessageStatus.DELIVERED.name, messageId)
            syncUser(contactData.userId)
        }
        if (messageDao.countMessageByQuoteId(data.conversationId, messageId) > 0) {
            messageDao.findMessageItemById(data.conversationId, messageId)?.let {
                messageDao.updateQuoteContentByQuoteId(data.conversationId, messageId, gson.toJson(it))
            }
        }
    }

    private fun requestResendKey(conversationId: String, userId: String, messageId: String) {
        val plainText = gson.toJson(TransferPlainData(
            action = PlainDataAction.RESEND_KEY.name,
            messageId = messageId
        ))
        val encoded = Base64.encodeBytes(plainText.toByteArray())
        val bm = createParamBlazeMessage(createPlainJsonParam(conversationId, userId, encoded))
        jobManager.addJobInBackground(SendPlaintextJob(bm, userId))

        val address = SignalProtocolAddress(userId, DEFAULT_DEVICE_ID)
        val ratchet = RatchetSenderKey(conversationId, address.toString(), RatchetStatus.REQUESTING.name,
            bm.params?.message_id, nowInUtc())
        ratchetSenderKeyDao.insert(ratchet)
    }

    private fun requestResendMessage(conversationId: String, userId: String) {
        val messages = messageDao.findFailedMessages(conversationId, userId) ?: return
        val plainText = gson.toJson(TransferPlainData(PlainDataAction.RESEND_MESSAGES.name, messages.reversed()))
        val encoded = Base64.encodeBytes(plainText.toByteArray())
        val bm = createParamBlazeMessage(createPlainJsonParam(conversationId, userId, encoded))
        jobManager.addJobInBackground(SendPlaintextJob(bm))
        ratchetSenderKeyDao.delete(conversationId, SignalProtocolAddress(userId, DEFAULT_DEVICE_ID).toString())
    }

    private fun updateRemoteMessageStatus(messageId: String, status: MessageStatus = MessageStatus.DELIVERED) {
        jobDao.insert(createAckJob(ACKNOWLEDGE_MESSAGE_RECEIPTS, BlazeAckMessage(messageId, status.name)))
    }

    private fun syncUser(userId: String) {
        val user = userDao.findUser(userId)
        if (user == null) {
            try {
                val call = userApi.getUserById(userId).execute()
                val response = call.body()
                if (response != null && response.isSuccess) {
                    response.data?.let { data ->
                        userDao.insert(data)
                    }
                }
            } catch (e: IOException) {
                WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshUserWorker>(
                    workDataOf(RefreshUserWorker.USER_IDS to arrayOf(userId)))
            }
        }
    }

    private fun refreshKeys(conversationId: String) {
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
            Log.w(TAG, "Registering new pre keys...")
        }
    }

    private fun sendNotificationJob(message: Message, source: String) {
        if (source == LIST_PENDING_MESSAGES) {
            return
        }
        if (MixinApplication.conversationId == message.conversationId) {
            return
        }
        jobManager.addJobInBackground(NotificationJob(message))
    }
}