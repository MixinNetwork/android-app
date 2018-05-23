package one.mixin.android.ui.conversation

import android.app.Activity
import android.app.NotificationManager
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import android.arch.paging.LivePagedListBuilder
import android.arch.paging.PagedList
import android.graphics.Bitmap
import android.net.Uri
import android.support.annotation.WorkerThread
import android.support.v4.util.ArraySet
import androidx.core.net.toUri
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import one.mixin.android.MixinApplication
import one.mixin.android.api.request.RelationshipRequest
import one.mixin.android.api.request.TransferRequest
import one.mixin.android.crypto.Base64
import one.mixin.android.extension.bitmap2String
import one.mixin.android.extension.blurThumbnail
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.createGifTemp
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getImageSize
import one.mixin.android.extension.getMimeType
import one.mixin.android.extension.getUriForFile
import one.mixin.android.extension.getVideoModel
import one.mixin.android.extension.getVideoPath
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
import one.mixin.android.util.Session
import one.mixin.android.util.image.Compressor
import one.mixin.android.util.video.MediaController
import one.mixin.android.vo.App
import one.mixin.android.vo.AssetItem
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.ConversationStatus
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Participant
import one.mixin.android.vo.User
import one.mixin.android.vo.createAttachmentMessage
import one.mixin.android.vo.createContactMessage
import one.mixin.android.vo.createConversation
import one.mixin.android.vo.createMediaMessage
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.createStickerMessage
import one.mixin.android.vo.createVideoMessage
import one.mixin.android.websocket.BlazeMessage
import one.mixin.android.websocket.TransferContactData
import one.mixin.android.websocket.TransferStickerData
import org.jetbrains.anko.doAsync
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

    fun getMessages(id: String): LiveData<PagedList<MessageItem>> {
        return LivePagedListBuilder(conversationRepository.getMessages(id), PagedList.Config.Builder()
            .setPageSize(100)
            .setEnablePlaceholders(true)
            .build())
            .build()
    }

    fun getMessagesMinimal(id: String) = conversationRepository.getMessagesMinimal(id)

    fun indexUnread(conversationId: String, userId: String) =
        conversationRepository.indexUnread(conversationId, userId)
            .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())!!

    fun searchConversationById(id: String) =
        conversationRepository.searchConversationById(id)
            .observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())!!

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

    fun sendAttachmentMessage(conversationId: String, sender: User, attachment: Attachment, isPlain: Boolean) {
        val category = if (isPlain) MessageCategory.PLAIN_DATA.name else MessageCategory.SIGNAL_DATA.name
        val message = createAttachmentMessage(UUID.randomUUID().toString(), conversationId, sender.userId, category,
            null, attachment.filename, attachment.uri.toString(),
            attachment.mimeType, attachment.fileSize, nowInUtc(), null,
            null, MediaStatus.PENDING, MessageStatus.SENDING)
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
        val message = createStickerMessage(UUID.randomUUID().toString(), conversationId, sender.userId, category,
            encoded, transferStickerData.albumId, transferStickerData.name, MessageStatus.SENDING, nowInUtc())
        jobManager.addJobInBackground(SendMessageJob(message))
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
            val videoFile = MixinApplication.get().getVideoPath()
                .createVideoTemp(
                    when {
                        video.needChange -> "mp4"
                        video.fileName.contains(".") -> video.fileName.substring(video.fileName.lastIndexOf(".") + 1)
                        else -> ""
                    })
            MediaController().convertVideo(video.originalPath, video.bitrate, video.resultWidth, video.resultHeight, video
                .originalWidth, video
                .originalHeight, videoFile, video.needChange)

            val message = createVideoMessage(UUID.randomUUID().toString(), conversationId, sender.userId,
                category, null, video.fileName, videoFile.toUri().toString(), video.duration, video
                .resultWidth,
                video.resultHeight, video.thumbnail, if (video.needChange) {
                "video/mp4"
            } else {
                getMimeType(uri)!!
            },
                videoFile.length(), nowInUtc(), null, null, MediaStatus.PENDING, MessageStatus.SENDING)
            jobManager.addJobInBackground(SendAttachmentMessageJob(message))
        }.observeOn(AndroidSchedulers.mainThread())!!

    fun sendFordVideoMessage(conversationId: String, sender: User, id: String, isPlain: Boolean) =
        Flowable.just(id).observeOn(Schedulers.io()).map {
            val category = if (isPlain) MessageCategory.PLAIN_VIDEO.name else MessageCategory.SIGNAL_VIDEO.name
            conversationRepository.findMessageById(id)?.let { message ->
                jobManager.addJobInBackground(SendAttachmentMessageJob(createVideoMessage(UUID.randomUUID().toString(),
                    conversationId, sender.userId, category, null, message.name, message.mediaUrl,
                    message.mediaDuration?.toLong(), message.mediaWidth, message.mediaHeight, message.thumbImage,
                    message.mediaMimeType!!, message.mediaSize!!, nowInUtc(), null, null,
                    MediaStatus.PENDING, MessageStatus.SENDING
                )))
            }
        }.observeOn(AndroidSchedulers.mainThread())!!

    fun sendFordDataMessage(conversationId: String, sender: User, id: String, isPlain: Boolean) =
        Flowable.just(id).observeOn(Schedulers.io()).map {
            val category = if (isPlain) MessageCategory.PLAIN_DATA.name else MessageCategory.SIGNAL_DATA.name
            conversationRepository.findMessageById(id)?.let { message ->
                val uri: Uri = if (message.userId != Session.getAccountId()) {
                    MixinApplication.appContext.getUriForFile(File(message.mediaUrl))
                } else {
                    Uri.parse(message.mediaUrl)
                }
                jobManager.addJobInBackground(SendAttachmentMessageJob(createAttachmentMessage(UUID.randomUUID().toString(), conversationId, sender.userId,
                    category, null, message.name, uri.toString(), message.mediaMimeType!!, message.mediaSize!!, nowInUtc(), null,
                    null, MediaStatus.PENDING, MessageStatus.SENDING)))
            }
        }.observeOn(AndroidSchedulers.mainThread())!!

    fun sendFordStickerMessage(conversationId: String, sender: User, id: String, isPlain: Boolean) =
        Flowable.just(id).observeOn(Schedulers.io()).map {
            conversationRepository.findMessageById(id)?.let { message ->
                sendStickerMessage(conversationId, sender, TransferStickerData(message.albumId!!, message.name!!), isPlain)
            }
        }.observeOn(AndroidSchedulers.mainThread())!!

    fun sendImageMessage(conversationId: String, sender: User, uri: Uri, isPlain: Boolean): Flowable<Int> {
        val category = if (isPlain) MessageCategory.PLAIN_IMAGE.name else MessageCategory.SIGNAL_IMAGE.name
        val mimeType = getMimeType(uri)
        if (mimeType == "image/gif") {
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
        val temp = MixinApplication.get().getImagePath().createImageTemp(type = if (mimeType == "image/png") {
            ".png"
        } else {
            ".jpg"
        })
        return Compressor()
            .setCompressFormat(if (mimeType == "image/png") {
                Bitmap.CompressFormat.PNG
            } else {
                Bitmap.CompressFormat.JPEG
            })
            .compressToFileAsFlowable(File(uri.getFilePath(MixinApplication.get())), temp.absolutePath)
            .map { imageFile ->
                val imageUrl = Uri.fromFile(temp).toString()
                val length = imageFile.length()
                if (length <= 0) {
                    return@map -1
                }
                if (mimeType == null || (mimeType != "image/png" &&
                        mimeType != "image/jpg" && mimeType != "image/jpeg")) {
                    return@map -2
                }
                val size = getImageSize(imageFile)
                val thumbnail = imageFile.blurThumbnail(size)?.bitmap2String(mimeType)

                val message = createMediaMessage(UUID.randomUUID().toString(),
                    conversationId, sender.userId, category, null, imageUrl,
                    mimeType, length, size.width, size.height, thumbnail, null, null,
                    nowInUtc(), MediaStatus.PENDING, MessageStatus.SENDING)
                jobManager.addJobInBackground(SendAttachmentMessageJob(message))
                return@map -0
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

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

    fun makeMessageReadByConversationId(conversationId: String, accountId: String) {
        conversationRepository.makeMessageReadByConversationId(conversationId, accountId)
    }

    fun findUnreadMessages(conversationId: String) =
        conversationRepository.findUnreadMessages(conversationId).subscribeOn(Schedulers.io())!!

    fun getFriends() = userRepository.findFriends()

    fun getConversations() = conversationRepository.conversation()

    private val notificationManager: NotificationManager by lazy {
        MixinApplication.appContext.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun deleteMessages(set: ArraySet<MessageItem>) {
        val data = ArraySet(set)
        Flowable.fromIterable(data).subscribeOn(Schedulers.io()).observeOn(Schedulers.io()).subscribe {
            conversationRepository.deleteMessage(it.messageId)
            jobManager.cancelJobById(it.messageId)
            notificationManager.cancel(it.userId.hashCode())
        }
    }

    fun getXIN(): AssetItem? = assetRepository.getXIN()

    fun transfer(transferRequest: TransferRequest) = assetRepository.transfer(transferRequest)

    fun getStickerAlbums() = accountRepository.getStickerAlbums()

    fun getStickers(id: String) = accountRepository.getStickers(id)

    fun recentStickers() = accountRepository.recentUsedStickers()

    fun updateStickerUsedAt(albumId: String, name: String) {
        doAsync {
            val cur = System.currentTimeMillis()
            accountRepository.updateUsedAt(albumId, name, cur.toString())
        }
    }

    fun getApp(conversationId: String, userId: String?): LiveData<List<App>> {
        return if (userId == null) {
            conversationRepository.getGroupConversationApp(conversationId)
        } else {
            conversationRepository.getConversationApp(userId)
        }
    }

    fun assetItemsWithBalance(): LiveData<List<AssetItem>> = assetRepository.assetItemsWithBalance()
}
