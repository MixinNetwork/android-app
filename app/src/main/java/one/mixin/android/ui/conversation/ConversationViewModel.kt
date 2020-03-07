package one.mixin.android.ui.conversation

import android.app.Activity
import android.app.NotificationManager
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import com.google.gson.Gson
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.Constants
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.extension.base64Encode
import one.mixin.android.extension.bitmap2String
import one.mixin.android.extension.blurThumbnail
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.deserialize
import one.mixin.android.extension.fileExists
import one.mixin.android.extension.getAttachment
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getImageSize
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.getUriForFile
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.isUUID
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.postOptimize
import one.mixin.android.extension.putString
import one.mixin.android.job.AttachmentDownloadJob
import one.mixin.android.job.ConvertVideoJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshStickerAlbumJob
import one.mixin.android.job.RefreshUserJob
import one.mixin.android.job.RemoveStickersJob
import one.mixin.android.job.SendAckMessageJob
import one.mixin.android.job.SendAttachmentMessageJob
import one.mixin.android.job.SendGiphyJob
import one.mixin.android.job.SendMessageJob
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.util.Attachment
import one.mixin.android.util.ControlledRunner
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.util.Session
import one.mixin.android.util.image.Compressor
import one.mixin.android.vo.AppItem
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageMinimal
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.User
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.createAppCardMessage
import one.mixin.android.vo.createAttachmentMessage
import one.mixin.android.vo.createAudioMessage
import one.mixin.android.vo.createContactMessage
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.createLiveMessage
import one.mixin.android.vo.createMediaMessage
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.createPostMessage
import one.mixin.android.vo.createRecallMessage
import one.mixin.android.vo.createReplyTextMessage
import one.mixin.android.vo.createStickerMessage
import one.mixin.android.vo.createVideoMessage
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.giphy.Gif
import one.mixin.android.vo.giphy.Image
import one.mixin.android.vo.isImage
import one.mixin.android.vo.isVideo
import one.mixin.android.vo.toQuoteMessageItem
import one.mixin.android.vo.toUser
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.CREATE_MESSAGE
import one.mixin.android.websocket.ContactMessagePayload
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.websocket.RecallMessagePayload
import one.mixin.android.websocket.StickerMessagePayload
import one.mixin.android.widget.gallery.MimeType
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import timber.log.Timber

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class ConversationViewModel
@Inject
internal constructor(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val jobManager: MixinJobManager,
    private val assetRepository: AssetRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    fun getMessages(id: String, initialLoadKey: Int = 0): LiveData<PagedList<MessageItem>> {
        return LivePagedListBuilder(
            conversationRepository.getMessages(id), PagedList.Config.Builder()
                .setPrefetchDistance(PAGE_SIZE * 2)
                .setPageSize(PAGE_SIZE)
                .setEnablePlaceholders(true)
                .build()
        )
            .setInitialLoadKey(initialLoadKey)
            .build()
    }

    suspend fun indexUnread(conversationId: String) =
        conversationRepository.indexUnread(conversationId) ?: 0

    suspend fun findFirstUnreadMessageId(conversationId: String, offset: Int): String? =
        conversationRepository.findFirstUnreadMessageId(conversationId, offset)

    fun searchConversationById(id: String) =
        conversationRepository.searchConversationById(id)
            .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())

    fun getConversationById(id: String) = conversationRepository.getConversationById(id)

    fun saveDraft(conversationId: String, text: String) = viewModelScope.launch {
        conversationRepository.saveDraft(conversationId, text)
    }

    fun findUserById(conversationId: String): LiveData<User> =
        userRepository.findUserById(conversationId)

    fun sendTextMessage(conversationId: String, sender: User, content: String, isPlain: Boolean) {
        val category =
            if (isPlain) MessageCategory.PLAIN_TEXT.name else MessageCategory.SIGNAL_TEXT.name
        val message = createMessage(
            UUID.randomUUID().toString(), conversationId,
            sender.userId, category, content.trim(), nowInUtc(), MessageStatus.SENDING.name
        )
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    fun sendPostMessage(conversationId: String, sender: User, content: String, isPlain: Boolean) {
        val category =
            if (isPlain) MessageCategory.PLAIN_POST.name else MessageCategory.SIGNAL_POST.name
        val message = createPostMessage(
            UUID.randomUUID().toString(), conversationId,
            sender.userId, category, content.trim(), content.postOptimize(), nowInUtc(), MessageStatus.SENDING.name
        )
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    fun sendAppCardMessage(conversationId: String, sender: User, content: String) {
        val message = createAppCardMessage(UUID.randomUUID().toString(), conversationId,
            sender.userId, content, nowInUtc(), MessageStatus.SENDING.name
        )
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    fun sendReplyMessage(
        conversationId: String,
        sender: User,
        content: String,
        replyMessage: MessageItem,
        isPlain: Boolean
    ) {
        val category =
            if (isPlain) MessageCategory.PLAIN_TEXT.name else MessageCategory.SIGNAL_TEXT.name
        val message = createReplyTextMessage(
            UUID.randomUUID().toString(),
            conversationId,
            sender.userId,
            category,
            content.trim(),
            nowInUtc(),
            MessageStatus.SENDING.name,
            replyMessage.messageId,
            Gson().toJson(QuoteMessageItem(replyMessage))
        )
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    fun sendAttachmentMessage(conversationId: String, sender: User, attachment: Attachment, isPlain: Boolean, replyMessage: MessageItem? = null) {
        val category = if (isPlain) MessageCategory.PLAIN_DATA.name else MessageCategory.SIGNAL_DATA.name
        val message = createAttachmentMessage(UUID.randomUUID().toString(), conversationId, sender.userId, category,
            null, attachment.filename, attachment.uri.toString(),
            attachment.mimeType, attachment.fileSize, nowInUtc(), null,
            null, MediaStatus.PENDING, MessageStatus.SENDING.name, replyMessage?.messageId, replyMessage?.toQuoteMessageItem())
        jobManager.addJobInBackground(SendAttachmentMessageJob(message))
    }

    fun sendAudioMessage(
        conversationId: String,
        sender: User,
        file: File,
        duration: Long,
        waveForm: ByteArray,
        isPlain: Boolean,
        replyMessage: MessageItem? = null
    ) {
        val category = if (isPlain) MessageCategory.PLAIN_AUDIO.name else MessageCategory.SIGNAL_AUDIO.name
        val message = createAudioMessage(UUID.randomUUID().toString(), conversationId, sender.userId, null, category,
            file.length(), Uri.fromFile(file).toString(), duration.toString(), nowInUtc(), waveForm, null, null,
            MediaStatus.PENDING, MessageStatus.SENDING.name, replyMessage?.messageId, replyMessage?.toQuoteMessageItem())
        jobManager.addJobInBackground(SendAttachmentMessageJob(message))
    }

    fun sendStickerMessage(
        conversationId: String,
        sender: User,
        transferStickerData: StickerMessagePayload,
        isPlain: Boolean
    ) {
        val category =
            if (isPlain) MessageCategory.PLAIN_STICKER.name else MessageCategory.SIGNAL_STICKER.name
        val encoded = GsonHelper.customGson.toJson(transferStickerData).base64Encode()
        transferStickerData.stickerId?.let {
            val message = createStickerMessage(
                UUID.randomUUID().toString(),
                conversationId,
                sender.userId,
                category,
                encoded,
                transferStickerData.albumId,
                it,
                transferStickerData.name,
                MessageStatus.SENDING.name,
                nowInUtc()
            )
            jobManager.addJobInBackground(SendMessageJob(message))
        }
    }

    fun sendContactMessage(conversationId: String, sender: User, shareUserId: String, isPlain: Boolean, replyMessage: MessageItem? = null) {
        val category = if (isPlain) MessageCategory.PLAIN_CONTACT.name else MessageCategory.SIGNAL_CONTACT.name
        val transferContactData = ContactMessagePayload(shareUserId)
        val encoded = GsonHelper.customGson.toJson(transferContactData).base64Encode()
        val message = createContactMessage(UUID.randomUUID().toString(), conversationId, sender.userId,
            category, encoded, shareUserId, MessageStatus.SENDING.name, nowInUtc(), replyMessage?.messageId, replyMessage?.toQuoteMessageItem())
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    fun sendVideoMessage(
        conversationId: String,
        senderId: String,
        uri: Uri,
        isPlain: Boolean,
        messageId: String? = null,
        createdAt: String? = null,
        replyMessage: MessageItem? = null
    ) {
        val mid = messageId ?: UUID.randomUUID().toString()
        jobManager.addJobInBackground(
            ConvertVideoJob(
                conversationId,
                senderId,
                uri,
                isPlain,
                mid,
                createdAt,
                replyMessage
            )
        )
    }

    fun sendRecallMessage(conversationId: String, sender: User, list: List<MessageItem>) {
        list.forEach { messageItem ->
            val transferRecallData = RecallMessagePayload(messageItem.messageId)
            val encoded = GsonHelper.customGson.toJson(transferRecallData).base64Encode()
            val message = createRecallMessage(
                UUID.randomUUID().toString(), conversationId, sender.userId,
                MessageCategory.MESSAGE_RECALL.name, encoded, MessageStatus.SENDING.name, nowInUtc()
            )
            jobManager.addJobInBackground(
                SendMessageJob(
                    message,
                    recallMessageId = messageItem.messageId
                )
            )
        }
    }

    private fun sendLiveMessage(
        conversationId: String,
        sender: User,
        transferLiveData: LiveMessagePayload,
        isPlain: Boolean
    ) {
        val category =
            if (isPlain) MessageCategory.PLAIN_LIVE.name else MessageCategory.SIGNAL_LIVE.name
        val encoded =
            GsonHelper.customGson.toJson(transferLiveData).base64Encode()
        val message = createLiveMessage(
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
            nowInUtc()
        )
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    fun sendGiphyMessage(
        conversationId: String,
        senderId: String,
        image: Image,
        isPlain: Boolean,
        previewUrl: String
    ) {
        val category =
            if (isPlain) MessageCategory.PLAIN_IMAGE.name else MessageCategory.SIGNAL_IMAGE.name
        jobManager.addJobInBackground(
            SendGiphyJob(
                conversationId, senderId, image.url, image.width, image.height,
                category, UUID.randomUUID().toString(), previewUrl, nowInUtc()
            )
        )
    }

    fun sendImageMessage(
        conversationId: String,
        sender: User,
        uri: Uri,
        isPlain: Boolean,
        mime: String? = null,
        replyMessage: MessageItem? = null
    ): Flowable<Int>? {
        val category =
            if (isPlain) MessageCategory.PLAIN_IMAGE.name else MessageCategory.SIGNAL_IMAGE.name
        var mimeType = mime
        if (mimeType == null) {
            mimeType = getMimeType(uri)
            if (mimeType?.isImageSupport() != true) {
                viewModelScope.launch {
                    MixinApplication.get().toast(R.string.error_format)
                }
                return null
            }
        }
        if (mimeType == MimeType.GIF.toString()) {
            return Flowable.just(uri).map {
                val gifFile = MixinApplication.get().getImagePath().createGifTemp()
                val path = uri.getFilePath(MixinApplication.get()) ?: return@map -1
                gifFile.copyFromInputStream(FileInputStream(path))
                val size = getImageSize(gifFile)
                val thumbnail = gifFile.blurThumbnail(size)?.bitmap2String(mimeType)

                val message = createMediaMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    category,
                    null,
                    Uri.fromFile(gifFile).toString(),
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
                    replyMessage?.toQuoteMessageItem()
                )
                jobManager.addJobInBackground(SendAttachmentMessageJob(message))
                return@map -0
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        }

        val temp = MixinApplication.get().getImagePath().createImageTemp(type = ".jpg")

        return Compressor()
            .setCompressFormat(Bitmap.CompressFormat.JPEG)
            .compressToFileAsFlowable(
                File(uri.getFilePath(MixinApplication.get())),
                temp.absolutePath
            )
            .map { imageFile ->
                val imageUrl = Uri.fromFile(temp).toString()
                val length = imageFile.length()
                if (length <= 0) {
                    return@map -1
                }
                val size = getImageSize(imageFile)
                val thumbnail = imageFile.blurThumbnail(size)?.bitmap2String(mimeType)
                val message = createMediaMessage(
                    UUID.randomUUID().toString(),
                    conversationId,
                    sender.userId,
                    category,
                    null,
                    imageUrl,
                    MimeType.JPEG.toString(),
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
                    replyMessage?.toQuoteMessageItem()
                )
                jobManager.addJobInBackground(SendAttachmentMessageJob(message))
                return@map -0
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    fun sendFordMessage(conversationId: String, sender: User, id: String, isPlain: Boolean) =
        Flowable.just(id).observeOn(Schedulers.io()).map {
            conversationRepository.findMessageById(id)?.let { message ->
                when {
                    message.category.endsWith("_IMAGE") -> {
                        val category =
                            if (isPlain) MessageCategory.PLAIN_IMAGE.name else MessageCategory.SIGNAL_IMAGE.name
                        if (message.mediaUrl?.fileExists() != true) {
                            return@let 0
                        }
                        jobManager.addJobInBackground(
                            SendAttachmentMessageJob(
                                createMediaMessage(
                                    UUID.randomUUID().toString(),
                                    conversationId,
                                    sender.userId,
                                    category,
                                    null,
                                    message.mediaUrl,
                                    message.mediaMimeType!!,
                                    message.mediaSize!!,
                                    message.mediaWidth,
                                    message.mediaHeight,
                                    message.thumbImage,
                                    null,
                                    null,
                                    nowInUtc(),
                                    MediaStatus.PENDING,
                                    MessageStatus.SENDING.name
                                )
                            )
                        )
                    }
                    message.category.endsWith("_LIVE") -> {
                        if (message.mediaWidth == null ||
                            message.mediaWidth == 0 ||
                            message.mediaHeight == null ||
                            message.mediaHeight == 0 ||
                            message.thumbUrl.isNullOrBlank() ||
                            message.mediaUrl.isNullOrBlank()
                        ) {
                            return@let 1
                        }
                        sendLiveMessage(
                            conversationId, sender, LiveMessagePayload(
                                message.mediaWidth,
                                message.mediaHeight,
                                message.thumbUrl,
                                message.mediaUrl
                            ), isPlain
                        )
                    }
                    message.category.endsWith("_VIDEO") -> {
                        val category =
                            if (isPlain) MessageCategory.PLAIN_VIDEO.name else MessageCategory.SIGNAL_VIDEO.name
                        if (message.mediaUrl?.fileExists() != true) {
                            return@let 0
                        }
                        val mediaDuration = try {
                            message.mediaDuration?.toLong()
                        } catch (e: Exception) {
                            0L
                        }
                        jobManager.addJobInBackground(
                            SendAttachmentMessageJob(
                                createVideoMessage(
                                    UUID.randomUUID().toString(),
                                    conversationId,
                                    sender.userId,
                                    category,
                                    null,
                                    message.name,
                                    message.mediaUrl,
                                    mediaDuration,
                                    message.mediaWidth,
                                    message.mediaHeight,
                                    message.thumbImage,
                                    message.mediaMimeType!!,
                                    message.mediaSize!!,
                                    nowInUtc(),
                                    null,
                                    null,
                                    MediaStatus.PENDING,
                                    MessageStatus.SENDING.name
                                )
                            )
                        )
                    }
                    message.category.endsWith("_DATA") -> {
                        val category =
                            if (isPlain) MessageCategory.PLAIN_DATA.name else MessageCategory.SIGNAL_DATA.name
                        val uri =
                            if (Uri.parse(message.mediaUrl).scheme == SCHEME_CONTENT) {
                                message.mediaUrl
                            } else {
                                MixinApplication.get().getUriForFile(File(message.mediaUrl))
                                    .toString()
                            }
                        jobManager.addJobInBackground(
                            SendAttachmentMessageJob(
                                createAttachmentMessage(UUID.randomUUID().toString(), conversationId, sender.userId, category, null,
                                    message.name, uri, message.mediaMimeType!!, message.mediaSize!!, nowInUtc(), null, null,
                                    MediaStatus.PENDING, MessageStatus.SENDING.name)
                            )
                        )
                    }
                    message.category.endsWith("_STICKER") -> {
                        sendStickerMessage(
                            conversationId,
                            sender,
                            StickerMessagePayload(
                                name = message.name,
                                stickerId = message.stickerId!!
                            ),
                            isPlain
                        )
                    }
                    message.category.endsWith("_AUDIO") -> {
                        val category =
                            if (isPlain) MessageCategory.PLAIN_AUDIO.name else MessageCategory.SIGNAL_AUDIO.name
                        if (message.mediaUrl?.fileExists() != true) {
                            return@let 0
                        }
                        jobManager.addJobInBackground(
                            SendAttachmentMessageJob(
                                createAudioMessage(
                                    UUID.randomUUID().toString(),
                                    conversationId,
                                    sender.userId,
                                    null,
                                    category,
                                    message.mediaSize!!,
                                    message.mediaUrl,
                                    message.mediaDuration!!,
                                    nowInUtc(),
                                    message.mediaWaveform!!,
                                    null,
                                    null,
                                    MediaStatus.PENDING,
                                    MessageStatus.SENDING.name
                                )
                            )
                        )
                    }
                }
                return@let 1
            }
        }.observeOn(AndroidSchedulers.mainThread())!!

    fun updateRelationship(request: RelationshipRequest) {
        jobManager.addJobInBackground(UpdateRelationshipJob(request))
    }

    fun getGroupParticipantsLiveData(conversationId: String) =
        conversationRepository.getGroupParticipantsLiveData(conversationId)

    fun initConversation(conversationId: String, recipient: User, sender: User) {
        val createdAt = nowInUtc()
        val conversation = createConversation(
            conversationId, ConversationCategory.CONTACT.name,
            recipient.userId, ConversationStatus.START.ordinal
        )
        val participants = arrayListOf(
            Participant(conversationId, sender.userId, "", createdAt),
            Participant(conversationId, recipient.userId, "", createdAt)
        )
        conversationRepository.syncInsertConversation(conversation, participants)
    }

    fun getUserById(userId: String) =
        Observable.just(userId).subscribeOn(Schedulers.io())
            .map { userRepository.getUserById(it) }.observeOn(AndroidSchedulers.mainThread())!!

    fun cancel(id: String) = viewModelScope.launch(Dispatchers.IO) {
        jobManager.cancelJobByMixinJobId(id) {
            viewModelScope.launch {
                conversationRepository.updateMediaStatus(MediaStatus.CANCELED.name, id)
            }
        }
    }

    fun retryUpload(id: String, onError: () -> Unit) {
        doAsync {
            conversationRepository.findMessageById(id)?.let {
                if (it.isVideo() && it.mediaSize != null && it.mediaSize == 0L) {
                    try {
                        jobManager.addJobInBackground(
                            ConvertVideoJob(
                                it.conversationId, it.userId, Uri.parse(it.mediaUrl),
                                it.category.startsWith("PLAIN"), it.id, it.createdAt
                            )
                        )
                    } catch (e: NullPointerException) {
                        onError.invoke()
                    }
                } else if (it.isImage() && it.mediaSize != null && it.mediaSize == 0L) { // un-downloaded GIPHY
                    val category =
                        if (it.category.startsWith("PLAIN")) MessageCategory.PLAIN_IMAGE.name else MessageCategory.SIGNAL_IMAGE.name
                    try {
                        jobManager.addJobInBackground(
                            SendGiphyJob(
                                it.conversationId, it.userId, it.mediaUrl!!,
                                it.mediaWidth!!, it.mediaHeight!!, category, it.id, it.thumbImage
                                    ?: "", it.createdAt
                            )
                        )
                    } catch (e: NullPointerException) {
                        onError.invoke()
                    }
                } else {
                    jobManager.addJobInBackground(SendAttachmentMessageJob(it))
                }
            }
        }
    }

    fun retryDownload(id: String) {
        doAsync {
            conversationRepository.findMessageById(id)?.let {
                jobManager.addJobInBackground(AttachmentDownloadJob(it))
            }
        }
    }

    fun markMessageRead(conversationId: String, accountId: String) {
        viewModelScope.launch(SINGLE_DB_THREAD) {
            conversationRepository.getUnreadMessage(conversationId, accountId).also { list ->
                if (list.isNotEmpty()) {
                    notificationManager.cancel(conversationId.hashCode())
                    conversationRepository.batchMarkReadAndTake(
                        conversationId,
                        Session.getAccountId()!!,
                        list.last().createdAt
                    )
                    list.map {
                        createAckJob(
                            ACKNOWLEDGE_MESSAGE_RECEIPTS,
                            BlazeAckMessage(it.id, MessageStatus.READ.name)
                        )
                    }.let {
                        conversationRepository.insertList(it)
                    }
                    createReadSessionMessage(list, conversationId)
                }
            }
        }
    }

    suspend fun getFriends(): List<User> = userRepository.getFriends()

    fun findFriendsNotBot() = userRepository.findFriendsNotBot()

    suspend fun successConversationList() = conversationRepository.successConversationList()

    fun findContactUsers() = userRepository.findContactUsers()

    private val notificationManager: NotificationManager by lazy {
        MixinApplication.appContext.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun deleteMessages(list: List<MessageItem>) {
        viewModelScope.launch(SINGLE_DB_THREAD) {
            list.forEach { item ->
                conversationRepository.deleteMessage(
                    item.messageId, item.mediaUrl
                )
                jobManager.cancelJobByMixinJobId(item.messageId)
                notificationManager.cancel(item.userId.hashCode())
            }
        }
    }

    fun getSystemAlbums() = accountRepository.getSystemAlbums()

    suspend fun getPersonalAlbums() = accountRepository.getPersonalAlbums()

    fun observeStickers(id: String) = accountRepository.observeStickers(id)

    fun observePersonalStickers() = accountRepository.observePersonalStickers()

    fun recentStickers() = accountRepository.recentUsedStickers()

    fun updateStickerUsedAt(stickerId: String) {
        viewModelScope.launch {
            accountRepository.updateUsedAt(stickerId, System.currentTimeMillis().toString())
        }
    }

    fun getApp(conversationId: String, userId: String?): LiveData<List<AppItem>> {
        return if (userId == null) {
            conversationRepository.getGroupConversationApp(conversationId)
        } else {
            conversationRepository.getConversationApp(userId, Session.getAccountId()!!)
        }
    }

    suspend fun findAppById(id: String) = userRepository.findAppById(id)

    fun assetItemsWithBalance(): LiveData<List<AssetItem>> =
        assetRepository.assetItemsWithBalance()

    fun addStickerAsync(stickerAddRequest: StickerAddRequest) =
        accountRepository.addStickerAsync(stickerAddRequest)

    fun addStickerLocal(sticker: Sticker, albumId: String) =
        accountRepository.addStickerLocal(sticker, albumId)

    fun removeStickers(ids: List<String>) {
        jobManager.addJobInBackground(RemoveStickersJob(ids))
    }

    fun refreshStickerAlbums() {
        jobManager.addJobInBackground(RefreshStickerAlbumJob())
    }

    suspend fun findMessageIndex(conversationId: String, messageId: String) =
        conversationRepository.findMessageIndex(conversationId, messageId)

    private fun findUnreadMessagesSync(conversationId: String) =
        conversationRepository.findUnreadMessagesSync(conversationId)

    private fun sendForwardMessages(
        conversationId: String,
        messages: List<ForwardMessage>?,
        isPlainMessage: Boolean
    ) {
        messages?.let { forwardMessages ->
            val sender = Session.getAccount()!!.toUser()
            for (item in forwardMessages) {
                if (item.id != null) {
                    sendFordMessage(conversationId, sender, item.id, isPlainMessage).subscribe({}, {
                        Timber.e("")
                    })
                } else {
                    when (item.type) {
                        ForwardCategory.CONTACT.name -> {
                            sendContactMessage(
                                conversationId,
                                sender,
                                item.sharedUserId!!,
                                isPlainMessage
                            )
                        }
                        ForwardCategory.IMAGE.name -> {
                            sendImageMessage(
                                conversationId,
                                sender,
                                Uri.parse(item.mediaUrl),
                                isPlainMessage
                            )
                                ?.subscribe({
                                }, {
                                    Timber.e(it)
                                })
                        }
                        ForwardCategory.DATA.name -> {
                            MixinApplication.get().getAttachment(Uri.parse(item.mediaUrl))?.let {
                                sendAttachmentMessage(conversationId, sender, it, isPlainMessage)
                            }
                        }
                        ForwardCategory.VIDEO.name -> {
                            sendVideoMessage(
                                conversationId,
                                sender.userId,
                                Uri.parse(item.mediaUrl),
                                isPlainMessage
                            )
                        }
                        ForwardCategory.TEXT.name -> {
                            item.content?.let {
                                sendTextMessage(conversationId, sender, it, isPlainMessage)
                            }
                        }
                        ForwardCategory.POST.name -> {
                            item.content?.let {
                                sendPostMessage(conversationId, sender, it, isPlainMessage)
                            }
                        }
                        ForwardCategory.APP_CARD.name -> {
                            item.content?.let {
                                sendAppCardMessage(conversationId, sender, it)
                            }
                        }
                    }
                }
            }
        }
    }

    fun sendForwardMessages(
        selectItem: List<Any>,
        messages: List<ForwardMessage>?,
        showSuccess: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var conversationId: String? = null
            for (item in selectItem) {
                if (item is User) {
                    conversationId =
                        conversationRepository.getConversationIdIfExistsSync(item.userId)
                    if (conversationId == null) {
                        conversationId =
                            generateConversationId(Session.getAccountId()!!, item.userId)
                        initConversation(conversationId, item, Session.getAccount()!!.toUser())
                    }
                    sendForwardMessages(conversationId, messages, item.isBot())
                } else if (item is ConversationItem) {
                    conversationId = item.conversationId
                    sendForwardMessages(item.conversationId, messages, item.isBot())
                }

                if (showSuccess) {
                    withContext(Dispatchers.Main) {
                        MixinApplication.get().toast(R.string.forward_success)
                    }
                }
                findUnreadMessagesSync(conversationId!!)?.let { list ->
                    if (list.isNotEmpty()) {
                        conversationRepository.batchMarkReadAndTake(
                            conversationId,
                            Session.getAccountId()!!,
                            list.last().createdAt
                        )
                        list.map { BlazeAckMessage(it.id, MessageStatus.READ.name) }
                            .let { messages ->
                                messages.chunked(100).forEach { list ->
                                    jobManager.addJobInBackground(SendAckMessageJob(list))
                                }
                            }
                        createReadSessionMessage(list, conversationId)
                    }
                }
            }
        }
    }

    private fun createReadSessionMessage(list: List<MessageMinimal>, conversationId: String) {
        Session.getExtensionSessionId()?.let {
            list.map {
                createAckJob(
                    CREATE_MESSAGE,
                    BlazeAckMessage(it.id, MessageStatus.READ.name),
                    conversationId
                )
            }.let {
                conversationRepository.insertList(it)
            }
        }
    }

    fun trendingGifs(limit: Int, offset: Int): Observable<List<Gif>> =
        accountRepository.trendingGifs(limit, offset).map { it.data }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun searchGifs(query: String, limit: Int, offset: Int): Observable<List<Gif>> =
        accountRepository.searchGifs(query, limit, offset).map { it.data }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

    fun observeAddress(addressId: String) = assetRepository.observeAddress(addressId)

    fun updateRecentUsedBots(
        defaultSharedPreferences: SharedPreferences,
        userId: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val botsString =
            defaultSharedPreferences.getString(Constants.Account.PREF_RECENT_USED_BOTS, null)
        if (botsString != null) {
            var botsList = botsString.split("=")
            if (botsList.size == 1 && !botsList[0].isUUID()) {
                getPreviousVersionBotsList(defaultSharedPreferences)?.let {
                    botsList = it
                }
            }
            if (botsList.isNullOrEmpty()) {
                defaultSharedPreferences.putString(Constants.Account.PREF_RECENT_USED_BOTS, userId)
                return@launch
            }

            val arr = botsList.filter { it != userId }
                .toMutableList()
                .also {
                    if (it.size >= Constants.RECENT_USED_BOTS_MAX_COUNT) {
                        it.dropLast(1)
                    }
                    it.add(0, userId)
                }
            defaultSharedPreferences.putString(
                Constants.Account.PREF_RECENT_USED_BOTS,
                arr.joinToString("=")
            )
        } else {
            defaultSharedPreferences.putString(Constants.Account.PREF_RECENT_USED_BOTS, userId)
        }
    }

    private fun getPreviousVersionBotsList(defaultSharedPreferences: SharedPreferences): List<String>? {
        defaultSharedPreferences.getString(
            Constants.Account.PREF_RECENT_USED_BOTS,
            null
        )?.let { botsString ->
            return botsString.deserialize<Array<String>>()?.toList()
        } ?: return null
    }

    suspend fun findLastMessage(conversationId: String) =
        conversationRepository.findLastMessage(conversationId)

    suspend fun findUnreadMessageByMessageId(
        conversationId: String,
        userId: String,
        messageId: String
    ) =
        conversationRepository.findUnreadMessageByMessageId(conversationId, userId, messageId)

    suspend fun isSilence(conversationId: String, userId: String) =
        conversationRepository.isSilence(conversationId, userId) == 0

    fun refreshUser(userId: String, forceRefresh: Boolean) {
        jobManager.addJobInBackground(RefreshUserJob(listOf(userId), forceRefresh = forceRefresh))
    }

    fun downloadAttachment(message: Message) {
        jobManager.addJobInBackground(AttachmentDownloadJob(message))
    }

    suspend fun suspendFindUserById(userId: String) = withContext(Dispatchers.IO) {
        userRepository.suspendFindUserById(userId)
    }

    suspend fun getSortMessagesByIds(messages: Set<MessageItem>): ArrayList<ForwardMessage> {
        return withContext(Dispatchers.IO) {
            val list = ArrayList<ForwardMessage>()
            list.addAll(conversationRepository.getSortMessagesByIds(messages.map { it.messageId }).map {
                when {
                    it.category.endsWith("_TEXT") -> ForwardMessage(
                        ForwardCategory.TEXT.name,
                        content = it.content
                    )
                    it.category.endsWith("_IMAGE") -> ForwardMessage(
                        ForwardCategory.IMAGE.name,
                        id = it.id
                    )
                    it.category.endsWith("_DATA") -> ForwardMessage(
                        ForwardCategory.DATA.name,
                        id = it.id
                    )
                    it.category.endsWith("_VIDEO") -> ForwardMessage(
                        ForwardCategory.VIDEO.name,
                        id = it.id
                    )
                    it.category.endsWith("_CONTACT") -> ForwardMessage(
                        ForwardCategory.CONTACT.name,
                        sharedUserId = it.sharedUserId
                    )
                    it.category.endsWith("_STICKER") -> ForwardMessage(
                        ForwardCategory.STICKER.name,
                        id = it.id
                    )
                    it.category.endsWith("_AUDIO") -> ForwardMessage(
                        ForwardCategory.AUDIO.name,
                        id = it.id
                    )
                    it.category.endsWith("_LIVE") -> ForwardMessage(
                        ForwardCategory.LIVE.name,
                        id = it.id
                    )
                    it.category.endsWith("_POST") -> ForwardMessage(
                        ForwardCategory.POST.name,
                        content = it.content
                    )
                    it.category == MessageCategory.APP_CARD.name -> ForwardMessage(
                        ForwardCategory.APP_CARD.name,
                        content = it.content
                    )
                    else -> ForwardMessage(ForwardCategory.TEXT.name)
                }
            })
            list
        }
    }

    suspend fun getAnnouncementByConversationId(conversationId: String) = conversationRepository.getAnnouncementByConversationId(conversationId)

    private val searchControlledRunner = ControlledRunner<List<User>>()

    suspend fun fuzzySearchUser(conversationId: String, keyword: String?): List<User> {
        return withContext(Dispatchers.IO) {
            searchControlledRunner.cancelPreviousThenRun {
                if (keyword.isNullOrEmpty()) {
                    userRepository.suspendGetGroupParticipants(conversationId)
                } else {
                    userRepository.fuzzySearchGroupUser(conversationId, keyword)
                }
            }
        }
    }

    suspend fun findUserByIdentityNumberSuspend(identityNumber: String) = userRepository.findUserByIdentityNumberSuspend(identityNumber)

    fun getUnreadMentionMessageByConversationId(conversationId: String) = conversationRepository.getUnreadMentionMessageByConversationId(conversationId)

    suspend fun markMentionRead(messageId: String, conversationId: String) {
        conversationRepository.markMentionRead(messageId, conversationId)
    }
}
