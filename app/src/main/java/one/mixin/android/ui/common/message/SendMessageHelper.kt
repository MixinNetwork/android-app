package one.mixin.android.ui.common.message

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.event.PinMessageEvent
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.encodeBlurHash
import one.mixin.android.extension.fromJson
import one.mixin.android.extension.getBotNumber
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getImageSize
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.postOptimize
import one.mixin.android.job.ConvertDataJob
import one.mixin.android.job.ConvertVideoJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendAttachmentMessageJob
import one.mixin.android.job.SendGiphyJob
import one.mixin.android.job.SendMessageJob
import one.mixin.android.job.SendTranscriptJob
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.util.Attachment
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.image.Compressor
import one.mixin.android.vo.AppCap
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.PinMessageMinimal
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.TranscriptMessage
import one.mixin.android.vo.TranscriptMinimal
import one.mixin.android.vo.User
import one.mixin.android.vo.createAppCardMessage
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
import one.mixin.android.vo.giphy.Image
import one.mixin.android.vo.isAppCard
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isLocation
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isSticker
import one.mixin.android.vo.isText
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.toCategory
import one.mixin.android.vo.toQuoteMessageItem
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.websocket.LocationPayload
import one.mixin.android.websocket.PinAction
import one.mixin.android.websocket.PinMessagePayload
import one.mixin.android.websocket.RecallMessagePayload
import one.mixin.android.websocket.StickerMessagePayload
import one.mixin.android.widget.gallery.MimeType
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class SendMessageHelper
    @Inject
    internal constructor(private val jobManager: MixinJobManager, private val userRepository: UserRepository, private val conversationRepository: ConversationRepository) {
        fun sendTextMessage(
            scope: CoroutineScope,
            conversationId: String,
            sender: User,
            content: String,
            encryptCategory: EncryptCategory,
            isSilentMessage: Boolean? = null,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_TEXT,
                    MessageCategory.SIGNAL_TEXT,
                    MessageCategory.ENCRYPTED_TEXT,
                )
            val message =
                createMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    category,
                    content.trim(),
                    nowInUtc(),
                    MessageStatus.SENDING.name,
                )
            scope.launch {
                val botNumber = message.content?.getBotNumber()
                var recipientId: String? = null
                if (!botNumber.isNullOrBlank()) {
                    userRepository.findAppByAppNumber(message.conversationId, botNumber)?.apply {
                        recipientId = appId
                        message.category =
                            if (capabilities?.contains(AppCap.ENCRYPTED.name) == true) {
                                MessageCategory.ENCRYPTED_TEXT.name
                            } else {
                                MessageCategory.PLAIN_TEXT.name
                            }
                    }
                }
                jobManager.addJobInBackground(SendMessageJob(message, recipientId = recipientId, isSilent = isSilentMessage))
            }
        }

        fun sendTranscriptMessage(
            messageId: String,
            conversationId: String,
            sender: User,
            transcriptMessages: List<TranscriptMessage>,
            encryptCategory: EncryptCategory,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_TRANSCRIPT,
                    MessageCategory.SIGNAL_TRANSCRIPT,
                    MessageCategory.ENCRYPTED_TRANSCRIPT,
                )
            transcriptMessages.onEach { t ->
                t.type =
                    when {
                        t.isText() ->
                            encryptCategory.toCategory(
                                MessageCategory.PLAIN_TEXT,
                                MessageCategory.SIGNAL_TEXT,
                                MessageCategory.ENCRYPTED_TEXT,
                            )
                        t.isAudio() ->
                            encryptCategory.toCategory(
                                MessageCategory.PLAIN_AUDIO,
                                MessageCategory.SIGNAL_AUDIO,
                                MessageCategory.ENCRYPTED_AUDIO,
                            )
                        t.isContact() ->
                            encryptCategory.toCategory(
                                MessageCategory.PLAIN_CONTACT,
                                MessageCategory.SIGNAL_CONTACT,
                                MessageCategory.ENCRYPTED_CONTACT,
                            )
                        t.isData() ->
                            encryptCategory.toCategory(
                                MessageCategory.PLAIN_DATA,
                                MessageCategory.SIGNAL_DATA,
                                MessageCategory.ENCRYPTED_DATA,
                            )
                        t.isImage() ->
                            encryptCategory.toCategory(
                                MessageCategory.PLAIN_IMAGE,
                                MessageCategory.SIGNAL_IMAGE,
                                MessageCategory.ENCRYPTED_IMAGE,
                            )
                        t.isLocation() ->
                            encryptCategory.toCategory(
                                MessageCategory.PLAIN_LOCATION,
                                MessageCategory.SIGNAL_LOCATION,
                                MessageCategory.ENCRYPTED_LOCATION,
                            )
                        t.isPost() ->
                            encryptCategory.toCategory(
                                MessageCategory.PLAIN_POST,
                                MessageCategory.SIGNAL_POST,
                                MessageCategory.ENCRYPTED_POST,
                            )
                        t.isSticker() ->
                            encryptCategory.toCategory(
                                MessageCategory.PLAIN_STICKER,
                                MessageCategory.SIGNAL_STICKER,
                                MessageCategory.ENCRYPTED_STICKER,
                            )
                        t.isVideo() ->
                            encryptCategory.toCategory(
                                MessageCategory.PLAIN_VIDEO,
                                MessageCategory.SIGNAL_VIDEO,
                                MessageCategory.ENCRYPTED_VIDEO,
                            )
                        t.isLive() ->
                            encryptCategory.toCategory(
                                MessageCategory.PLAIN_LIVE,
                                MessageCategory.SIGNAL_LIVE,
                                MessageCategory.ENCRYPTED_LIVE,
                            )
                        t.isAppCard() -> MessageCategory.APP_CARD.name
                        else -> throw IllegalArgumentException("Unknown type")
                    }
            }
            val message =
                createMessage(
                    messageId,
                    conversationId,
                    sender.userId,
                    category,
                    GsonHelper.customGson.toJson(
                        transcriptMessages.sortedBy { t -> t.createdAt }.map {
                            TranscriptMinimal(it.userFullName ?: "", it.type, it.content)
                        },
                    ),
                    nowInUtc(),
                    MessageStatus.SENDING.name,
                )
            jobManager.addJobInBackground(SendTranscriptJob(message, transcriptMessages))
        }

        fun sendReplyTextMessage(
            conversationId: String,
            sender: User,
            content: String,
            replyMessage: MessageItem,
            encryptCategory: EncryptCategory,
            isSilentMessage: Boolean? = null,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_TEXT,
                    MessageCategory.SIGNAL_TEXT,
                    MessageCategory.ENCRYPTED_TEXT,
                )
            val message =
                createReplyTextMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    category,
                    content.trim(),
                    nowInUtc(),
                    MessageStatus.SENDING.name,
                    replyMessage.messageId,
                    Gson().toJson(QuoteMessageItem(replyMessage)),
                )
            jobManager.addJobInBackground(SendMessageJob(message, isSilent = isSilentMessage))
        }

        fun sendPostMessage(
            conversationId: String,
            sender: User,
            content: String,
            encryptCategory: EncryptCategory,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_POST,
                    MessageCategory.SIGNAL_POST,
                    MessageCategory.ENCRYPTED_POST,
                )
            val message =
                createPostMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    category,
                    content.trim(),
                    content.postOptimize(),
                    nowInUtc(),
                    MessageStatus.SENDING.name,
                )
            jobManager.addJobInBackground(SendMessageJob(message))
        }

    fun sendAppCardMessage(
        conversationId: String,
        sender: User,
        content: String,
    ) {
        val message =
            createAppCardMessage(
                UUID.randomUUID().toString(),
                conversationId,
                sender.userId,
                content,
                nowInUtc(),
                MessageStatus.SENDING.name,
            )
        jobManager.addJobInBackground(SendMessageJob(message))
    }

        fun sendAttachmentMessage(
            conversationId: String,
            sender: User,
            attachment: Attachment,
            encryptCategory: EncryptCategory,
            replyMessage: MessageItem? = null,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_DATA,
                    MessageCategory.SIGNAL_DATA,
                    MessageCategory.ENCRYPTED_DATA,
                )
            val message =
                createAttachmentMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    category,
                    null,
                    attachment.filename,
                    attachment.uri.toString(),
                    attachment.mimeType,
                    attachment.fileSize,
                    nowInUtc(),
                    null,
                    null,
                    MediaStatus.PENDING,
                    MessageStatus.SENDING.name,
                    replyMessage?.messageId,
                    replyMessage?.toQuoteMessageItem(),
                )
            jobManager.addJobInBackground(ConvertDataJob(message))
        }

        fun sendAudioMessage(
            conversationId: String,
            messageId: String,
            sender: User,
            file: File,
            duration: Long,
            waveForm: ByteArray,
            encryptCategory: EncryptCategory,
            replyMessage: MessageItem? = null,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_AUDIO,
                    MessageCategory.SIGNAL_AUDIO,
                    MessageCategory.ENCRYPTED_AUDIO,
                )
            val message =
                createAudioMessage(
                    messageId,
                    conversationId,
                    sender.userId,
                    null,
                    category,
                    file.length(),
                    file.name,
                    duration.toString(),
                    nowInUtc(),
                    waveForm,
                    null,
                    null,
                    MediaStatus.PENDING,
                    MessageStatus.SENDING.name,
                    replyMessage?.messageId,
                    replyMessage?.toQuoteMessageItem(),
                )
            jobManager.addJobInBackground(SendAttachmentMessageJob(message))
        }

        fun sendStickerMessage(
            conversationId: String,
            sender: User,
            stickerId: String,
            encryptCategory: EncryptCategory,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_STICKER,
                    MessageCategory.SIGNAL_STICKER,
                    MessageCategory.ENCRYPTED_STICKER,
                )
            val transferStickerData = StickerMessagePayload(stickerId)
            val encoded = GsonHelper.customGson.toJson(transferStickerData).base64Encode()
            val message =
                createStickerMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    category,
                    encoded,
                    stickerId,
                    MessageStatus.SENDING.name,
                    nowInUtc(),
                )
            jobManager.addJobInBackground(SendMessageJob(message))
        }

        fun sendContactMessage(
            conversationId: String,
            sender: User,
            shareUserId: String,
            shareUserFullName: String? = null,
            encryptCategory: EncryptCategory,
            replyMessage: MessageItem? = null,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_CONTACT,
                    MessageCategory.SIGNAL_CONTACT,
                    MessageCategory.ENCRYPTED_CONTACT,
                )
            val transferContactData = ContactMessagePayload(shareUserId)
            val encoded = GsonHelper.customGson.toJson(transferContactData).base64Encode()
            val message =
                createContactMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    category,
                    encoded,
                    shareUserId,
                    MessageStatus.SENDING.name,
                    nowInUtc(),
                    shareUserFullName,
                    replyMessage?.messageId,
                    replyMessage?.toQuoteMessageItem(),
                )
            jobManager.addJobInBackground(SendMessageJob(message))
        }

        fun sendVideoMessage(
            conversationId: String,
            senderId: String,
            uri: Uri,
            start: Float,
            end: Float,
            encryptCategory: EncryptCategory,
            messageId: String? = null,
            createdAt: String? = null,
            replyMessage: MessageItem? = null,
        ) {
            val mid = messageId ?: UUID.randomUUID().toString()
            jobManager.addJobInBackground(ConvertVideoJob(conversationId, senderId, uri, start, end, encryptCategory, mid, createdAt, replyMessage))
        }

        fun sendRecallMessage(
            conversationId: String,
            sender: User,
            list: List<MessageItem>,
        ) {
            list.forEach { messageItem ->
                val transferRecallData = RecallMessagePayload(messageItem.messageId)
                val encoded = GsonHelper.customGson.toJson(transferRecallData).base64Encode()
                val message =
                    createMessage(
                        UUID.randomUUID().toString(),
                        conversationId,
                        sender.userId,
                        MessageCategory.MESSAGE_RECALL.name,
                        encoded,
                        nowInUtc(),
                        MessageStatus.SENDING.name,
                    )
                jobManager.addJobInBackground(
                    SendMessageJob(
                        message,
                        recallMessageId = messageItem.messageId,
                    ),
                )
            }
        }

        fun sendUnPinMessage(
            conversationId: String,
            sender: User,
            messageIds: List<String>,
        ) {
            val transferPinData = PinMessagePayload(PinAction.UNPIN.name, messageIds)
            val encoded = GsonHelper.customGson.toJson(transferPinData).base64Encode()
            val message =
                createMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    MessageCategory.MESSAGE_PIN.name,
                    encoded,
                    nowInUtc(),
                    MessageStatus.SENDING.name,
                )
            jobManager.addJobInBackground(
                SendMessageJob(
                    message,
                ),
            )
        }

        fun sendPinMessage(
            conversationId: String,
            sender: User,
            action: PinAction,
            list: Collection<PinMessageMinimal>,
        ) {
            if (list.isEmpty() || list.size > 128) {
                return
            }
            val transferPinData = PinMessagePayload(action.name, list.map { it.messageId })
            val encoded = GsonHelper.customGson.toJson(transferPinData).base64Encode()
            val message =
                createMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    MessageCategory.MESSAGE_PIN.name,
                    encoded,
                    nowInUtc(),
                    MessageStatus.SENDING.name,
                )
            if (action == PinAction.PIN) {
                list.forEachIndexed { index, msg ->
                    val category = msg.type
                    val content =
                        if (msg.isText()) {
                            msg.content
                        } else {
                            null
                        }
                    val mId = UUID.randomUUID().toString()
                    conversationRepository.insertMessage(
                        createPinMessage(
                            mId,
                            conversationId,
                            sender.userId,
                            msg.messageId, // quote pinned message id
                            PinMessageMinimal(msg.messageId, category, content),
                            nowInUtc(),
                            MessageStatus.READ.name,
                        ),
                    )
                    if (msg.isText()) {
                        conversationRepository.syncMention(msg.messageId, mId)
                    }
                    if (index == list.size - 1) {
                        RxBus.publish(PinMessageEvent(conversationId, msg.messageId))
                    }
                }
            }
            // Notify pin message
            MessageFlow.update(conversationId, list.map { it.messageId })
            jobManager.addJobInBackground(
                SendMessageJob(
                    message,
                ),
            )
        }

        fun sendLiveMessage(
            conversationId: String,
            sender: User,
            transferLiveData: LiveMessagePayload,
            encryptCategory: EncryptCategory,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_LIVE,
                    MessageCategory.SIGNAL_LIVE,
                    MessageCategory.ENCRYPTED_LIVE,
                )
            val encoded =
                GsonHelper.customGson.toJson(transferLiveData)
            val message =
                createLiveMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    category,
                    encoded,
                    transferLiveData.width,
                    transferLiveData.height,
                    transferLiveData.url,
                    transferLiveData.thumbUrl,
                    MessageStatus.SENDING.name,
                    nowInUtc(),
                )
            jobManager.addJobInBackground(SendMessageJob(message))
        }

        fun sendGiphyMessage(
            conversationId: String,
            senderId: String,
            image: Image,
            encryptCategory: EncryptCategory,
            previewUrl: String,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_IMAGE,
                    MessageCategory.SIGNAL_IMAGE,
                    MessageCategory.ENCRYPTED_IMAGE,
                )
            jobManager.addJobInBackground(
                SendGiphyJob(
                    conversationId,
                    senderId,
                    image.url,
                    image.width,
                    image.height,
                    image.size.toLong(),
                    category,
                    UUID.randomUUID().toString(),
                    previewUrl,
                    nowInUtc(),
                ),
            )
        }

        fun sendLocationMessage(
            conversationId: String,
            senderId: String,
            location: LocationPayload,
            encryptCategory: EncryptCategory,
        ) {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_LOCATION,
                    MessageCategory.SIGNAL_LOCATION,
                    MessageCategory.ENCRYPTED_LOCATION,
                )
            jobManager.addJobInBackground(
                SendMessageJob(
                    createLocationMessage(
                        UUID.randomUUID().toString(),
                        conversationId,
                        senderId,
                        category,
                        GsonHelper.customGson.toJson(location),
                        MessageStatus.SENT.name,
                        nowInUtc(),
                    ),
                ),
            )
        }

        /**
         * Send image message
         *
         * @return 0  send successful
         * @return -1 target image has some bad data
         * @return -2 target image format not supported
         */
        fun sendImageMessage(
            conversationId: String,
            messageId: String,
            sender: User,
            uri: Uri,
            encryptCategory: EncryptCategory,
            notCompress: Boolean,
            mime: String? = null,
            replyMessage: MessageItem? = null,
            fromInput: Boolean = false,
        ): Int {
            val category =
                encryptCategory.toCategory(
                    MessageCategory.PLAIN_IMAGE,
                    MessageCategory.SIGNAL_IMAGE,
                    MessageCategory.ENCRYPTED_IMAGE,
                )
            var mimeType = mime
            if (mimeType == null) {
                mimeType = getMimeType(uri, true)
                if (mimeType?.isImageSupport() != true) {
                    return -2
                }
            }
            if (mimeType == MimeType.GIF.toString()) {
                val gifFile = MixinApplication.get().getImagePath().createGifTemp(conversationId, messageId)
                val path = uri.getFilePath(MixinApplication.get()) ?: return -1
                gifFile.copyFromInputStream(FileInputStream(path))
                val size = getImageSize(gifFile)
                val thumbnail = gifFile.encodeBlurHash()

                val message =
                    createMediaMessage(
                        messageId,
                        conversationId,
                        sender.userId,
                        category,
                        null,
                        gifFile.name,
                        mimeType,
                        gifFile.length(),
                        size.width,
                        size.height,
                        thumbnail,
                        null,
                        null,
                        nowInUtc(),
                        MediaStatus.PENDING,
                        MessageStatus.SENDING.name,
                        replyMessage?.messageId,
                        replyMessage?.toQuoteMessageItem(),
                    )
                jobManager.addJobInBackground(SendAttachmentMessageJob(message))
                return 0
            }
            val newMimeType = if (fromInput) MimeType.WEBP else MimeType.JPG
            val temp =
                MixinApplication.get().getImagePath().createImageTemp(
                    conversationId,
                    messageId,
                    type =
                        if (fromInput) {
                            ".webp"
                        } else {
                            ".jpg"
                        },
                )
            val imageFile: File =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && mimeType == MimeType.HEIC.toString()) {
                    val source = ImageDecoder.createSource(MixinApplication.get().contentResolver, uri)
                    val bitmap = ImageDecoder.decodeBitmap(source)
                    temp.outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    }
                    temp
                } else {
                    try {
                        Compressor()
                            .setCompressFormat(
                                if (fromInput) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                        Bitmap.CompressFormat.WEBP_LOSSLESS
                                    } else {
                                        @Suppress("DEPRECATION")
                                        Bitmap.CompressFormat.WEBP
                                    }
                                } else {
                                    Bitmap.CompressFormat.JPEG
                                },
                            )
                            .setQuality(if (notCompress) 100 else 85)
                            .compressToFile(uri, temp.absolutePath)
                    } catch (e: IOException) {
                        Timber.e("compress image ${e.stackTraceToString()}")
                        return -1
                    }
                }
            val length = imageFile.length()
            if (length <= 0) {
                return -1
            }
            val size = getImageSize(imageFile)
            val thumbnail = imageFile.encodeBlurHash()
            val message =
                createMediaMessage(
                    messageId,
                    conversationId,
                    sender.userId,
                    category,
                    null,
                    temp.name,
                    newMimeType.toString(),
                    length,
                    size.width,
                    size.height,
                    thumbnail,
                    null,
                    null,
                    nowInUtc(),
                    MediaStatus.PENDING,
                    MessageStatus.SENDING.name,
                    replyMessage?.messageId,
                    replyMessage?.toQuoteMessageItem(),
                )
            jobManager.addJobInBackground(SendAttachmentMessageJob(message))
            return 0
        }
    }
