package one.mixin.android.ui.conversation

import android.app.Activity
import android.app.NotificationManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.WorkerThread
import androidx.collection.ArraySet
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import one.mixin.android.Constants.PAGE_SIZE
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.StickerAddRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.bitmap2String
import one.mixin.android.extension.blurThumbnail
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.extension.fileExists
import one.mixin.android.extension.getAttachment
import one.mixin.android.extension.getFileNameNoEx
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getImageSize
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.getVideoModel
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.AttachmentDownloadJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.SendAckMessageJob
import one.mixin.android.job.SendAttachmentMessageJob
import one.mixin.android.job.SendMessageJob
import one.mixin.android.job.UpdateRelationshipJob
import one.mixin.android.repository.AccountRepository
import one.mixin.android.repository.AssetRepository
import one.mixin.android.repository.ConversationRepository
import one.mixin.android.repository.UserRepository
import one.mixin.android.util.Attachment
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.SINGLE_DB_THREAD
import one.mixin.android.util.Session
import one.mixin.android.util.encryptPin
import one.mixin.android.util.image.Compressor
import one.mixin.android.util.video.MediaController
import one.mixin.android.vo.App
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationItem
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.Sticker
import one.mixin.android.vo.User
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.createAttachmentMessage
import one.mixin.android.vo.createAudioMessage
import one.mixin.android.vo.createContactMessage
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.createMediaMessage
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.createReplyMessage
import one.mixin.android.vo.createStickerMessage
import one.mixin.android.vo.createVideoMessage
import one.mixin.android.vo.generateConversationId
import one.mixin.android.vo.giphy.Gif
import one.mixin.android.vo.toUser
import one.mixin.android.websocket.ACKNOWLEDGE_MESSAGE_RECEIPTS
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.TransferContactData
import one.mixin.android.websocket.TransferStickerData
import one.mixin.android.websocket.createAckListParamBlazeMessage
import one.mixin.android.widget.gallery.MimeType
import one.mixin.android.work.RefreshStickerAlbumWorker
import one.mixin.android.work.RemoveStickersWorker
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject

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
        return LivePagedListBuilder(conversationRepository.getMessages(id), PagedList.Config.Builder()
            .setPrefetchDistance(PAGE_SIZE * 2)
            .setPageSize(PAGE_SIZE)
            .setEnablePlaceholders(true)
            .build())
            .setInitialLoadKey(initialLoadKey)
            .build()
    }

    fun indexUnread(conversationId: String) =
        conversationRepository.indexUnread(conversationId)

    fun searchConversationById(id: String) =
        conversationRepository.searchConversationById(id)
            .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())!!

    private fun getConversationIdIfExistsSync(recipientId: String) = conversationRepository.getConversationIdIfExistsSync(recipientId)

    fun getConversationById(id: String) = conversationRepository.getConversationById(id)

    fun saveDraft(conversationId: String, text: String) =
        conversationRepository.saveDraft(conversationId, text)

    fun findUserByConversationId(conversationId: String): LiveData<User> =
        userRepository.findUserByConversationId(conversationId)

    fun findUserById(conversationId: String): LiveData<User> = userRepository.findUserById(conversationId)

    fun sendAckMessage(blazeMessage: BlazeMessage) {
        jobManager.addJobInBackground(SendAckMessageJob(blazeMessage))
    }

    fun sendTextMessage(conversationId: String, sender: User, content: String, isPlain: Boolean) {
        val category = if (isPlain) MessageCategory.PLAIN_TEXT.name else MessageCategory.SIGNAL_TEXT.name
        val message = createMessage(UUID.randomUUID().toString(), conversationId,
            sender.userId, category, content.trim(), nowInUtc(), MessageStatus.SENDING)
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    fun sendReplyMessage(conversationId: String, sender: User, content: String, replyMessage: MessageItem, isPlain: Boolean) {
        val category = if (isPlain) MessageCategory.PLAIN_TEXT.name else MessageCategory.SIGNAL_TEXT.name
        val message = createReplyMessage(UUID.randomUUID().toString(), conversationId,
            sender.userId, category, content.trim(), nowInUtc(), MessageStatus.SENDING, replyMessage.messageId, Gson().toJson(QuoteMessageItem(replyMessage)))
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    fun sendAttachmentMessage(conversationId: String, sender: User, attachment: Attachment, isPlain: Boolean) {
        val category = if (isPlain) MessageCategory.PLAIN_DATA.name else MessageCategory.SIGNAL_DATA.name
        val message = createAttachmentMessage(UUID.randomUUID().toString(), conversationId, sender.userId, category,
            null, attachment.filename, attachment.uri.toString(),
            attachment.mimeType, attachment.fileSize, nowInUtc(), null,
            null, MediaStatus.PENDING, MessageStatus.SENDING)
        jobManager.addJobInBackground(SendAttachmentMessageJob(message))
    }

    fun sendAudioMessage(conversationId: String, sender: User, file: File, duration: Long, waveForm: ByteArray, isPlain: Boolean) {
        val category = if (isPlain) MessageCategory.PLAIN_AUDIO.name else MessageCategory.SIGNAL_AUDIO.name
        val message = createAudioMessage(UUID.randomUUID().toString(), conversationId, sender.userId, null, category,
            file.length(), Uri.fromFile(file).toString(), duration.toString(), nowInUtc(), waveForm, null, null,
            MediaStatus.PENDING, MessageStatus.SENDING)
        jobManager.addJobInBackground(SendAttachmentMessageJob(message))
    }

    fun sendStickerMessage(
        conversationId: String,
        sender: User,
        transferStickerData: TransferStickerData,
        isPlain: Boolean
    ) {
        val category = if (isPlain) MessageCategory.PLAIN_STICKER.name else MessageCategory.SIGNAL_STICKER.name
        val encoded = Base64.encodeBytes(GsonHelper.customGson.toJson(transferStickerData).toByteArray())
        transferStickerData.stickerId?.let {
            val message = createStickerMessage(UUID.randomUUID().toString(), conversationId, sender.userId, category,
                encoded, transferStickerData.albumId, it, transferStickerData.name, MessageStatus.SENDING, nowInUtc())
            jobManager.addJobInBackground(SendMessageJob(message))
        }
    }

    fun sendContactMessage(conversationId: String, sender: User, shareUserId: String, isPlain: Boolean) {
        val category = if (isPlain) MessageCategory.PLAIN_CONTACT.name else MessageCategory.SIGNAL_CONTACT.name
        val transferContactData = TransferContactData(shareUserId)
        val encoded = Base64.encodeBytes(GsonHelper.customGson.toJson(transferContactData).toByteArray())
        val message = createContactMessage(UUID.randomUUID().toString(), conversationId, sender.userId,
            category, encoded, shareUserId, MessageStatus.SENDING, nowInUtc())
        jobManager.addJobInBackground(SendMessageJob(message))
    }

    fun sendVideoMessage(conversationId: String, sender: User, uri: Uri, isPlain: Boolean) =
        Flowable.just(uri).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).map {
            val video = MixinApplication.appContext.getVideoModel(it)!!
            val category = if (isPlain) MessageCategory.PLAIN_VIDEO.name else MessageCategory.SIGNAL_VIDEO.name
            val mimeType = getMimeType(uri)
            if (mimeType != "video/mp4") {
                video.needChange = true
            }
            if (!video.fileName.endsWith(".mp4")) {
                video.fileName = "${video.fileName.getFileNameNoEx()}.mp4"
            }
            val videoFile = MixinApplication.get().getVideoPath().createVideoTemp("mp4")

            MediaController().convertVideo(video.originalPath, video.bitrate, video.resultWidth, video.resultHeight, video
                .originalWidth, video
                .originalHeight, videoFile, video.needChange)

            val message = createVideoMessage(UUID.randomUUID().toString(), conversationId, sender.userId,
                category, null, video.fileName, videoFile.toUri().toString(), video.duration, video
                .resultWidth,
                video.resultHeight, video.thumbnail,
                "video/mp4",
                videoFile.length(), nowInUtc(), null, null, MediaStatus.PENDING, MessageStatus.SENDING)
            jobManager.addJobInBackground(SendAttachmentMessageJob(message))
        }.observeOn(AndroidSchedulers.mainThread())!!

    fun sendImageMessage(conversationId: String, sender: User, uri: Uri, isPlain: Boolean): Flowable<Int>? {
        val category = if (isPlain) MessageCategory.PLAIN_IMAGE.name else MessageCategory.SIGNAL_IMAGE.name
        val mimeType = getMimeType(uri)
        if (mimeType?.isImageSupport() != true) {
            MixinApplication.get().toast(R.string.error_format)
            return null
        }
        if (mimeType == MimeType.GIF.toString()) {
            return Flowable.just(uri).map {
                val gifFile = MixinApplication.get().getImagePath().createGifTemp()
                gifFile.copyFromInputStream(FileInputStream(uri.getFilePath(MixinApplication.get())))
                val size = getImageSize(gifFile)
                val thumbnail = gifFile.blurThumbnail(size)?.bitmap2String(mimeType)

                val message = createMediaMessage(UUID.randomUUID().toString(),
                    conversationId, sender.userId, category, null, Uri.fromFile(gifFile).toString(),
                    mimeType, gifFile.length(), size.width, size.height, thumbnail, null, null,
                    nowInUtc(), MediaStatus.PENDING, MessageStatus.SENDING)
                jobManager.addJobInBackground(SendAttachmentMessageJob(message))
                return@map -0
            }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
        }

        val temp = MixinApplication.get().getImagePath().createImageTemp(type = ".jpg")

        return Compressor()
            .setCompressFormat(Bitmap.CompressFormat.JPEG)
            .compressToFileAsFlowable(File(uri.getFilePath(MixinApplication.get())), temp.absolutePath)
            .map { imageFile ->
                val imageUrl = Uri.fromFile(temp).toString()
                val length = imageFile.length()
                if (length <= 0) {
                    return@map -1
                }
                val size = getImageSize(imageFile)
                val thumbnail = imageFile.blurThumbnail(size)?.bitmap2String(mimeType)

                val message = createMediaMessage(UUID.randomUUID().toString(),
                    conversationId, sender.userId, category, null, imageUrl,
                    MimeType.JPEG.toString(), length, size.width, size.height, thumbnail, null, null,
                    nowInUtc(), MediaStatus.PENDING, MessageStatus.SENDING)
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
                        val category = if (isPlain) MessageCategory.PLAIN_IMAGE.name else MessageCategory.SIGNAL_IMAGE.name
                        if (message.mediaUrl?.fileExists() != true) {
                            return@let 0
                        }
                        jobManager.addJobInBackground(SendAttachmentMessageJob(createMediaMessage(UUID.randomUUID().toString(),
                            conversationId, sender.userId, category, null, message.mediaUrl, message.mediaMimeType!!, message.mediaSize!!,
                            message.mediaWidth, message.mediaHeight, message.thumbImage, null, null, nowInUtc(),
                            MediaStatus.PENDING, MessageStatus.SENDING
                        )))
                    }
                    message.category.endsWith("_VIDEO") -> {
                        val category = if (isPlain) MessageCategory.PLAIN_VIDEO.name else MessageCategory.SIGNAL_VIDEO.name
                        if (message.mediaUrl?.fileExists() != true) {
                            return@let 0
                        }
                        jobManager.addJobInBackground(SendAttachmentMessageJob(createVideoMessage(UUID.randomUUID().toString(),
                            conversationId, sender.userId, category, null, message.name, message.mediaUrl,
                            message.mediaDuration?.toLong(), message.mediaWidth, message.mediaHeight, message.thumbImage,
                            message.mediaMimeType!!, message.mediaSize!!, nowInUtc(), null, null,
                            MediaStatus.PENDING, MessageStatus.SENDING
                        )))
                    }
                    message.category.endsWith("_DATA") -> {
                        val category = if (isPlain) MessageCategory.PLAIN_DATA.name else MessageCategory.SIGNAL_DATA.name
                        val uri = if (message.userId == Session.getAccountId()) {
                            if (message.mediaUrl?.fileExists() != true) {
                                return@let 0
                            }
                            message.mediaUrl
                        } else {
                            val file = File(message.mediaUrl).apply {
                                if (!this.exists()) {
                                    return@let 0
                                }
                            }
                            file.toUri().toString()
                        }

                        jobManager.addJobInBackground(SendAttachmentMessageJob(createAttachmentMessage(UUID.randomUUID().toString(), conversationId, sender.userId,
                            category, null, message.name, uri, message.mediaMimeType!!, message.mediaSize!!, nowInUtc(), null,
                            null, MediaStatus.PENDING, MessageStatus.SENDING)))
                    }
                    message.category.endsWith("_STICKER") -> {
                        sendStickerMessage(conversationId, sender, TransferStickerData(name = message.name, stickerId = message.stickerId!!), isPlain)
                    }
                    message.category.endsWith("_AUDIO") -> {
                        val category = if (isPlain) MessageCategory.PLAIN_AUDIO.name else MessageCategory.SIGNAL_AUDIO.name
                        if (message.mediaUrl?.fileExists() != true) {
                            return@let 0
                        }
                        jobManager.addJobInBackground(SendAttachmentMessageJob(createAudioMessage(UUID.randomUUID().toString(), conversationId, sender.userId,
                            null, category, message.mediaSize!!, message.mediaUrl, message.mediaDuration!!, nowInUtc(), message.mediaWaveform!!, null,
                            null, MediaStatus.PENDING, MessageStatus.SENDING)))
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

    @WorkerThread
    fun initConversation(conversationId: String, recipient: User, sender: User) {
        val createdAt = nowInUtc()
        val conversation = createConversation(conversationId, ConversationCategory.CONTACT.name,
            recipient.userId, ConversationStatus.START.ordinal)
        val participants = arrayListOf(
            Participant(conversationId, sender.userId, "", createdAt),
            Participant(conversationId, recipient.userId, "", createdAt)
        )
        conversationRepository.syncInsertConversation(conversation, participants)
    }

    fun getUserById(userId: String) =
        Observable.just(userId).subscribeOn(Schedulers.io())
            .map { userRepository.getUserById(it) }.observeOn(AndroidSchedulers.mainThread())!!

    fun cancel(id: String) {
        doAsync {
            notNullElse(jobManager.findJobById(id), { it.cancel() }, {
                conversationRepository.updateMediaStatusStatus(MediaStatus.CANCELED.name, id)
            })
        }
    }

    fun retryUpload(id: String) {
        doAsync {
            conversationRepository.findMessageById(id)?.let {
                jobManager.addJobInBackground(SendAttachmentMessageJob(it))
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
        GlobalScope.launch(SINGLE_DB_THREAD) {
            conversationRepository.getUnreadMessage(conversationId, accountId)?.also { list ->
                if (list.isNotEmpty()) {
                    notificationManager.cancel(conversationId.hashCode())
                    conversationRepository.batchMarkReadAndTake(conversationId, Session.getAccountId()!!, list.last().created_at)
                    list.map { createAckJob(ACKNOWLEDGE_MESSAGE_RECEIPTS, BlazeAckMessage(it.id, MessageStatus.READ.name)) }.let {
                        conversationRepository.insertList(it)
                    }
                }
            }
        }
    }

    fun getFriends() = userRepository.findFriends()

    fun finFriendsNotBot() = userRepository.findFriendsNotBot()

    fun getConversations() = conversationRepository.conversation()

    fun findContactUsers() = userRepository.findContactUsers()

    private val notificationManager: NotificationManager by lazy {
        MixinApplication.appContext.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun deleteMessages(set: ArraySet<MessageItem>) {
        val data = ArraySet(set)
        GlobalScope.launch(SINGLE_DB_THREAD) {
            data.forEach { item ->
                conversationRepository.deleteMessage(item.messageId)
                jobManager.cancelJobById(item.messageId)
                notificationManager.cancel(item.userId.hashCode())
            }
        }
    }

    fun getXIN(): AssetItem? = assetRepository.getXIN()

    fun transfer(assetId: String, userId: String, amount: String, code: String, trace: String?, memo: String?) =
        assetRepository.transfer(TransferRequest(assetId, userId, amount, encryptPin(Session.getPinToken()!!, code), trace, memo))
            .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())!!

    fun getSystemAlbums() = accountRepository.getSystemAlbums()

    fun getPersonalAlbums() = accountRepository.getPersonalAlbums()

    fun observeStickers(id: String) = accountRepository.observeStickers(id)

    fun observePersonalStickers() = accountRepository.observePersonalStickers()

    fun recentStickers() = accountRepository.recentUsedStickers()

    fun updateStickerUsedAt(stickerId: String) {
        doAsync {
            val cur = System.currentTimeMillis()
            accountRepository.updateUsedAt(stickerId, cur.toString())
        }
    }

    fun getApp(conversationId: String, userId: String?): LiveData<List<App>> {
        return if (userId == null) {
            conversationRepository.getGroupConversationApp(conversationId)
        } else {
            conversationRepository.getConversationApp(userId)
        }
    }

    fun findAppById(id: String) = userRepository.findAppById(id)

    fun assetItemsWithBalance(): LiveData<List<AssetItem>> = assetRepository.assetItemsWithBalance()

    fun addSticker(stickerAddRequest: StickerAddRequest) = accountRepository.addSticker(stickerAddRequest)

    fun addStickerLocal(sticker: Sticker, albumId: String) = accountRepository.addStickerLocal(sticker, albumId)

    fun removeStickers(ids: List<String>) {
        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RemoveStickersWorker>(
            workDataOf(RemoveStickersWorker.STICKER_IDS to ids.toTypedArray()))
    }

    fun refreshStickerAlbums() {
        WorkManager.getInstance().enqueueOneTimeNetworkWorkRequest<RefreshStickerAlbumWorker>()
    }

    fun findMessageIndexSync(conversationId: String, messageId: String) =
        conversationRepository.findMessageIndex(conversationId, messageId)

    fun findMessageIndex(conversationId: String, messageId: String) =
        Observable.just(1).map {
            conversationRepository.findMessageIndex(conversationId, messageId)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())!!

    private fun findUnreadMessagesSync(conversationId: String) = conversationRepository.findUnreadMessagesSync(conversationId)

    private fun sendForwardMessages(conversationId: String, messages: List<ForwardMessage>?, isPlainMessage: Boolean) {
        messages?.let {
            val sender = Session.getAccount()!!.toUser()
            for (item in it) {
                if (item.id != null) {
                    sendFordMessage(conversationId, sender, item.id, isPlainMessage).subscribe({}, {
                        Timber.e("")
                    })
                } else {
                    when (item.type) {
                        ForwardCategory.CONTACT.name -> {
                            sendContactMessage(item.sharedUserId!!, sender, item.sharedUserId, isPlainMessage)
                        }
                        ForwardCategory.IMAGE.name -> {
                            sendImageMessage(conversationId, sender, Uri.parse(item.mediaUrl), isPlainMessage)
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
                            sendVideoMessage(conversationId, sender,
                                Uri.parse(item.mediaUrl), isPlainMessage)
                                .subscribe({
                                }, {
                                    Timber.e(it)
                                })
                        }
                        ForwardCategory.TEXT.name -> {
                            item.content?.let {
                                sendTextMessage(conversationId, sender, it, isPlainMessage)
                            }
                        }
                    }
                }
            }
        }
    }

    fun sendForwardMessages(selectItem: List<Any>, messages: List<ForwardMessage>?) {
        GlobalScope.launch(SINGLE_DB_THREAD) {
            var conversationId: String? = null
            for (item in selectItem) {
                if (item is User) {
                    conversationId = getConversationIdIfExistsSync(item.userId)
                    if (conversationId == null) {
                        conversationId = generateConversationId(Session.getAccountId()!!, item.userId)
                        initConversation(conversationId, item, Session.getAccount()!!.toUser())
                    }
                    sendForwardMessages(conversationId, messages, item.isBot())
                } else if (item is ConversationItem) {
                    conversationId = item.conversationId
                    sendForwardMessages(item.conversationId, messages, item.isBot())
                }

                MixinApplication.get().mainThread {
                    MixinApplication.get().toast(R.string.forward_success)
                }
                findUnreadMessagesSync(conversationId!!)?.let {
                    if (it.isNotEmpty()) {
                        conversationRepository.batchMarkReadAndTake(conversationId, Session.getAccountId()!!, it.last().created_at)
                        it.map { BlazeAckMessage(it.id, MessageStatus.READ.name) }.let {
                            it.chunked(100).forEach {
                                jobManager.addJobInBackground(SendAckMessageJob(createAckListParamBlazeMessage(it)))
                            }
                        }
                    }
                }
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
}