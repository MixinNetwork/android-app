@file:Suppress("DEPRECATION")
@file:UnstableApi

package one.mixin.android.vo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Parcelable
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.paging.PositionalDataSource
import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.Constants.DEFAULT_THUMB_IMAGE
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.decodeBase64
import one.mixin.android.extension.encodeBitmap
import one.mixin.android.extension.getPublicDownloadPath
import one.mixin.android.extension.getPublicMusicPath
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.hasWritePermission
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.timeFormat
import one.mixin.android.extension.toast
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.VideoPlayer
import one.mixin.android.util.blurhash.Base83
import one.mixin.android.util.blurhash.BlurHashEncoder
import one.mixin.android.util.reportException
import one.mixin.android.websocket.LiveMessagePayload
import one.mixin.android.websocket.toLocationData
import timber.log.Timber
import java.io.File

@SuppressLint("ParcelCreator")
@Entity
@Parcelize
data class MessageItem(
    @PrimaryKey
    val messageId: String,
    val conversationId: String,
    val userId: String,
    val userFullName: String,
    val userIdentityNumber: String,
    override val type: String?,
    val content: String?,
    val createdAt: String,
    val status: String,
    val mediaStatus: String?,
    val userAvatarUrl: String?,
    val mediaName: String?,
    val mediaMimeType: String?,
    val mediaSize: Long?,
    val thumbUrl: String?,
    val mediaWidth: Int?,
    val mediaHeight: Int?,
    val thumbImage: String?,
    val mediaUrl: String?,
    val mediaDuration: String?,
    val participantFullName: String?,
    val participantUserId: String?,
    val actionName: String?,
    val snapshotId: String?,
    val snapshotType: String?,
    val snapshotMemo: String?,
    val snapshotAmount: String?,
    val assetId: String?,
    val assetType: String?,
    val assetSymbol: String?,
    val assetIcon: String?,
    val assetCollectionHash: String?,
    val assetUrl: String?,
    val assetHeight: Int?,
    val assetWidth: Int?,
    @Deprecated(
        "Deprecated at database version 15",
        ReplaceWith("@{link sticker_id}", "one.mixin.android.vo.MessageItem.stickerId"),
        DeprecationLevel.ERROR,
    )
    val albumId: String?,
    val stickerId: String?,
    val assetName: String?,
    val appId: String?,
    val siteName: String? = null,
    val siteTitle: String? = null,
    val siteDescription: String? = null,
    val siteImage: String? = null,
    val sharedUserId: String? = null,
    val sharedUserFullName: String? = null,
    val sharedUserIdentityNumber: String? = null,
    val sharedUserAvatarUrl: String? = null,
    val sharedUserIsVerified: Boolean? = null,
    val sharedUserAppId: String? = null,
    val mediaWaveform: ByteArray? = null,
    val quoteId: String? = null,
    val quoteContent: String? = null,
    val groupName: String? = null,
    val mentions: String? = null,
    val mentionRead: Boolean? = null,
    val isPin: Boolean? = null,
    val expireIn: Long? = null,
    val expireAt: Long? = null,
) : Parcelable, ICategory {
    @IgnoredOnParcel
    @Ignore
    private var appCardShareable: Boolean? = null

    @IgnoredOnParcel
    val appCardData: AppCardData? by lazy {
        content?.let {
            GsonHelper.customGson.fromJson(it, AppCardData::class.java)
        }
    }

    val formatMemo: FormatMemo?
        get() {
            return if (snapshotMemo.isNullOrBlank()) {
                null
            } else {
                FormatMemo(snapshotMemo)
            }
        }

    fun isShareable(): Boolean? {
        if (type != MessageCategory.APP_CARD.name &&
            type != MessageCategory.PLAIN_LIVE.name &&
            type != MessageCategory.SIGNAL_LIVE.name &&
            type != MessageCategory.ENCRYPTED_LIVE.name &&
            type != MessageCategory.PLAIN_AUDIO.name &&
            type != MessageCategory.SIGNAL_AUDIO.name &&
            type != MessageCategory.ENCRYPTED_AUDIO.name
        ) {
            return null
        }

        try {
            if (type == MessageCategory.APP_CARD.name && appCardShareable == null) {
                appCardShareable = appCardData?.shareable
            } else if ((type == MessageCategory.PLAIN_LIVE.name || type == MessageCategory.SIGNAL_LIVE.name || type == MessageCategory.ENCRYPTED_LIVE.name) && appCardShareable == null) {
                appCardShareable =
                    GsonHelper.customGson.fromJson(
                        content,
                        LiveMessagePayload::class.java,
                    ).shareable
            } else if ((type == MessageCategory.PLAIN_AUDIO.name || type == MessageCategory.SIGNAL_AUDIO.name || type == MessageCategory.ENCRYPTED_AUDIO.name) && appCardShareable == null) {
                appCardShareable =
                    GsonHelper.customGson.fromJson(
                        content,
                        AttachmentExtra::class.java,
                    ).shareable
            }
        } catch (ignore: Exception) {
        }

        return appCardShareable
    }

    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<MessageItem>() {
                override fun areItemsTheSame(
                    oldItem: MessageItem,
                    newItem: MessageItem,
                ) =
                    oldItem.messageId == newItem.messageId

                override fun areContentsTheSame(
                    oldItem: MessageItem,
                    newItem: MessageItem,
                ) =
                    oldItem == newItem
            }
    }

    fun canNotForward() =
        this.type == MessageCategory.APP_BUTTON_GROUP.name ||
            this.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name ||
            this.type == MessageCategory.SYSTEM_SAFE_SNAPSHOT.name ||
            this.type == MessageCategory.SYSTEM_SAFE_INSCRIPTION.name ||
            this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
            this.type == MessageCategory.MESSAGE_PIN.name ||
            isCallMessage() || isRecall() || isGroupCall() || unfinishedAttachment() ||
            (isTranscript() && this.mediaStatus != MediaStatus.DONE.name) ||
            (isLive() && isShareable() == false)

    fun canNotReply() =
        this.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name ||
            this.type == MessageCategory.SYSTEM_SAFE_SNAPSHOT.name ||
            this.type == MessageCategory.SYSTEM_SAFE_INSCRIPTION.name ||
            this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
            this.type == MessageCategory.MESSAGE_PIN.name ||
            unfinishedAttachment() ||
            isCallMessage() || isRecall() || isGroupCall()

    fun canNotPin() =
        this.canNotReply() || this.type == MessageCategory.MESSAGE_PIN.name || (status != MessageStatus.SENT.name && status != MessageStatus.DELIVERED.name && status != MessageStatus.READ.name)

    private fun unfinishedAttachment(): Boolean = !mediaDownloaded(this.mediaStatus) && (isData() || isImage() || isVideo() || isAudio())
}

fun create(
    type: String,
    createdAt: String? = null,
) =
    MessageItem(
        "",
        "",
        "",
        "",
        "",
        type,
        null,
        createdAt
            ?: nowInUtc(),
        MessageStatus.READ.name,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
    )

fun MessageItem.isCallMessage() =
    type == MessageCategory.WEBRTC_AUDIO_CANCEL.name ||
        type == MessageCategory.WEBRTC_AUDIO_DECLINE.name ||
        type == MessageCategory.WEBRTC_AUDIO_END.name ||
        type == MessageCategory.WEBRTC_AUDIO_BUSY.name ||
        type == MessageCategory.WEBRTC_AUDIO_FAILED.name

fun MessageItem.isGroupCall() = type?.isGroupCallType() == true

fun MessageItem.supportSticker(): Boolean = isSticker() || isImage()

fun String.isGroupCallType() =
    this == MessageCategory.KRAKEN_END.name ||
        this == MessageCategory.KRAKEN_DECLINE.name ||
        this == MessageCategory.KRAKEN_CANCEL.name ||
        this == MessageCategory.KRAKEN_INVITE.name

fun MessageItem.isLottie() = assetType?.equals(Sticker.STICKER_TYPE_JSON, true) == true

fun MessageItem.mediaDownloaded() =
    mediaStatus == MediaStatus.DONE.name || mediaStatus == MediaStatus.READ.name

fun MessageItem.showVerifiedOrBot(
    verifiedView: View,
    botView: View,
) {
    when {
        sharedUserIsVerified == true -> {
            verifiedView.isVisible = true
            botView.isVisible = false
        }
        sharedUserAppId != null -> {
            verifiedView.isVisible = false
            botView.isVisible = true
        }
        else -> {
            verifiedView.isVisible = false
            botView.isVisible = false
        }
    }
}

fun MessageItem.shareFile(
    context: Context,
    mediaMimeType: String,
) {
    if (!hasWritePermission()) return

    val filePath = absolutePath()
    if (filePath == null) {
        reportException(IllegalStateException("Share messageItem failure, category: $type, mediaUrl: $mediaUrl, absolutePath: $filePath)}"))
        toast(R.string.File_error)
        return
    }

    val file = filePath.toUri().toFile()
    if (!file.exists()) {
        reportException(IllegalStateException("Share messageItem failure, category: $type, mediaUrl: $mediaUrl, absolutePath: $filePath)}"))
        toast(R.string.File_error)
        return
    }
    val uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file)

    val share = Intent()
    share.action = Intent.ACTION_SEND
    share.type = mediaMimeType
    share.putExtra(Intent.EXTRA_STREAM, uri)
    context.startActivity(Intent.createChooser(share, context.getString(R.string.Share)))
}

suspend fun MessageItem.saveToLocal(context: Context) {
    val filePath = absolutePath()
    if (filePath == null) {
        reportException(IllegalStateException("Save messageItem failure, category: $type, mediaUrl: $mediaUrl, absolutePath: null)}"))
        toast(R.string.Save_failure)
        return
    }

    val file = filePath.toUri().toFile()
    if (!file.exists()) {
        reportException(IllegalStateException("Save messageItem failure, category: $type, mediaUrl: $mediaUrl, absolutePath: $filePath)}"))
        toast(R.string.Save_failure)
        return
    }

    var str = R.string.Save_to_Gallery
    val outFile =
        if (MimeTypes.isVideo(mediaMimeType) || mediaMimeType?.isImageSupport() == true) {
            val dir = context.getPublicPicturePath()
            dir.mkdirs()
            File(dir, mediaName ?: file.name)
        } else {
            val dir =
                if (MimeTypes.isAudio(mediaMimeType)) {
                    str = R.string.Save_to_Music
                    context.getPublicMusicPath()
                } else {
                    str = R.string.Save_to_Downloads
                    context.getPublicDownloadPath()
                }
            dir.mkdirs()
            File(dir, mediaName ?: file.name)
        }
    if (outFile.isDirectory) {
        toast(R.string.Save_failure)
        return
    }
    withContext(Dispatchers.IO) {
        outFile.copyFromInputStream(file.inputStream())
    }
    MediaScannerConnection.scanFile(context, arrayOf(outFile.toString()), null, null)
    toast(str)
}

fun MessageItem.loadVideoOrLive(actionAfterLoad: (() -> Unit)? = null) {
    absolutePath()?.let {
        if (isLive()) {
            VideoPlayer.player().loadHlsVideo(it, messageId)
        } else {
            VideoPlayer.player().loadVideo(it, messageId)
        }
        actionAfterLoad?.invoke()
    }
}

fun MessageItem.toSimpleChat(): String {
    return "${createdAt.timeFormat()} : $userFullName - ${simpleChat()}"
}

private fun MessageItem.simpleChat(): String {
    return when {
        isText() -> {
            return content!!
        }
        isSticker() -> "[STICKER]"
        isImage() -> mediaUrl.notNullWithElse({ "[IMAGE - ${File(it).name}]" }, "[IMAGE]")
        isVideo() -> mediaUrl.notNullWithElse({ "[VIDEO - ${File(it).name}]" }, "[VIDEO]")
        isData() -> mediaUrl.notNullWithElse({ "[FILE - ${File(it).name}]" }, "[FILE]")
        isAudio() -> mediaUrl.notNullWithElse({ "[AUDIO - ${File(it).name}]" }, "[AUDIO]")
        type == MessageCategory.APP_BUTTON_GROUP.name -> "[Mixin APP]"
        type == MessageCategory.APP_CARD.name -> "[Mixin APP]"
        type == MessageCategory.SYSTEM_SAFE_INSCRIPTION.name -> "[COLLECTION]"
        type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name || type == MessageCategory.SYSTEM_SAFE_SNAPSHOT.name ->
            "[TRANSFER ${
                if (snapshotAmount?.toFloat()!! > 0) {
                    "+"
                } else {
                    ""
                }
            }$snapshotAmount $assetSymbol]"
        isContact() -> {
            "[CONTACT - $sharedUserFullName]"
        }
        isLive() -> "[LIVE]"
        isPost() -> content!!
        isLocation() -> "[LOCATION https://maps.google.com/?q=${toLocationData(content).run { "${this?.latitude}&${this?.longitude}" }}]"
        else -> throw IllegalArgumentException()
    }
}

class FixedMessageDataSource<T : Any>(private val items: List<T>, private val totalCount: Int) :
    PositionalDataSource<T>() {
    override fun loadRange(
        params: LoadRangeParams,
        callback: LoadRangeCallback<T>,
    ) {
        callback.onResult(items)
    }

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<T>,
    ) {
        callback.onResult(items, 0, totalCount)
    }
}

fun MessageItem.toTranscript(transcriptId: String): TranscriptMessage {
    val thumb =
        if ((thumbImage?.length ?: 0) > Constants.MAX_THUMB_IMAGE_LENGTH) {
            DEFAULT_THUMB_IMAGE
        } else if (thumbImage != null && !Base83.isValid(thumbImage)) {
            try {
                val bitmap = thumbImage.decodeBase64().encodeBitmap()
                BlurHashEncoder.encode(requireNotNull(bitmap))
            } catch (e: Exception) {
                thumbImage
            }
        } else {
            thumbImage
        }
    return TranscriptMessage(
        transcriptId,
        messageId,
        userId,
        userFullName,
        requireNotNull(type),
        createdAt,
        content,
        absolutePath() ?: assetUrl,
        mediaName,
        mediaSize,
        mediaWidth,
        mediaHeight,
        mediaMimeType,
        mediaDuration?.toLong(),
        mediaStatus,
        mediaWaveform,
        thumb,
        thumbUrl,
        null,
        null,
        null,
        stickerId,
        sharedUserId,
        mentions,
        quoteId,
        quoteContent,
    )
}

fun MessageItem.absolutePath(context: Context = MixinApplication.appContext): String? {
    return absolutePath(context, conversationId, mediaUrl)
}

fun MessageItem.mediaExists(context: Context = MixinApplication.appContext): Boolean {
    return try {
        val path = absolutePath(context) ?: return false
        return Uri.parse(path).toFile().exists()
    } catch (e: Exception) {
        Timber.e(e)
        false
    }
}
