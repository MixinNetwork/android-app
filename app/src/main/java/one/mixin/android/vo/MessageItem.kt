package one.mixin.android.vo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Parcelable
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.paging.PositionalDataSource
import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.parcelize.Parcelize
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.hasWritePermission
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.timeFormat
import one.mixin.android.extension.toast
import one.mixin.android.util.VideoPlayer
import one.mixin.android.websocket.toLocationData
import java.io.File
import java.io.FileInputStream
import java.lang.IllegalArgumentException

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
    val snapshotAmount: String?,
    val assetId: String?,
    val assetType: String?,
    val assetSymbol: String?,
    val assetIcon: String?,
    val assetUrl: String?,
    val assetHeight: Int?,
    val assetWidth: Int?,
    @Deprecated(
        "Deprecated at database version 15",
        ReplaceWith("@{link sticker_id}", "one.mixin.android.vo.MessageItem.stickerId"),
        DeprecationLevel.ERROR
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
    val mentionRead: Boolean? = null
) : Parcelable, ICategory {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MessageItem>() {
            override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem) =
                oldItem.messageId == newItem.messageId

            override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem) =
                oldItem == newItem
        }
    }

    fun canNotForward() = this.type == MessageCategory.APP_BUTTON_GROUP.name ||
        this.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name ||
        this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
        unfinishedAttachment() ||
        isCallMessage() || isRecall()

    fun canNotReply() =
        this.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name ||
            this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
            unfinishedAttachment() ||
            isCallMessage() || isRecall()

    fun unfinishedAttachment(): Boolean = !mediaDownloaded(this.mediaStatus) && (isData() || isImage() || isVideo() || isAudio())
}

fun create(type: String, createdAt: String? = null) = MessageItem(
    "", "", "", "", "",
    type, null,
    createdAt
        ?: nowInUtc(),
    MessageStatus.READ.name, null, null,
    null, null, null, null, null, null, null, null,
    null, null, null, null, null, null, null, null,
    null, null, null, null, null, null, null, null, null, null
)

fun MessageItem.canNotReply() =
    this.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name ||
        this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
        (!mediaDownloaded(this.mediaStatus) && this.isMedia()) ||
        isCallMessage() || isRecall()

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

fun MessageItem.mediaDownloaded() = mediaStatus == MediaStatus.DONE.name || mediaStatus == MediaStatus.READ.name

fun MessageItem.showVerifiedOrBot(verifiedView: View, botView: View) {
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

fun MessageItem.saveToLocal(context: Context) {
    if (!hasWritePermission()) return

    val filePath = mediaUrl?.toUri()?.getFilePath()
    if (filePath == null) {
        MixinApplication.appContext.toast(R.string.save_failure)
        return
    }

    val file = File(filePath)
    val outFile = if (MimeTypes.isVideo(mediaMimeType) || mediaMimeType?.isImageSupport() == true) {
        File(context.getPublicPicturePath(), mediaName ?: file.name)
    } else {
        val dir = if (MimeTypes.isAudio(mediaMimeType)) {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }
        dir.mkdir()
        File(dir, mediaName ?: file.name)
    }
    outFile.copyFromInputStream(FileInputStream(file))
    context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(outFile)))
    MixinApplication.appContext.toast(MixinApplication.appContext.getString(R.string.save_to, outFile.absolutePath))
}

fun MessageItem.loadVideoOrLive(actionAfterLoad: (() -> Unit)? = null) {
    mediaUrl?.let {
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
        type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name ->
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

class FixedMessageDataSource(private val messageItems: List<MessageItem>) : PositionalDataSource<MessageItem>() {
    override fun loadRange(params: LoadRangeParams, callback: LoadRangeCallback<MessageItem>) {
        callback.onResult(messageItems)
    }

    override fun loadInitial(
        params: LoadInitialParams,
        callback: LoadInitialCallback<MessageItem>
    ) {
        callback.onResult(messageItems, 0, 1)
    }
}

fun MessageItem.toTranscript(isPlain: Boolean = false): Transcript {
    val category = when {
        isImage() -> if (isPlain) MessageCategory.PLAIN_IMAGE.name else MessageCategory.SIGNAL_IMAGE.name
        isVideo() -> if (isPlain) MessageCategory.PLAIN_VIDEO.name else MessageCategory.SIGNAL_VIDEO.name
        isAudio() -> if (isPlain) MessageCategory.PLAIN_AUDIO.name else MessageCategory.SIGNAL_AUDIO.name
        isData() -> if (isPlain) MessageCategory.PLAIN_DATA.name else MessageCategory.SIGNAL_DATA.name
        isSticker() -> if (isPlain) MessageCategory.PLAIN_STICKER.name else MessageCategory.SIGNAL_STICKER.name
        isLive() -> if (isPlain) MessageCategory.PLAIN_LIVE.name else MessageCategory.SIGNAL_LIVE.name
        isLocation() -> if (isPlain) MessageCategory.PLAIN_LOCATION.name else MessageCategory.SIGNAL_LOCATION.name
        isContact() -> if (isPlain) MessageCategory.PLAIN_CONTACT.name else MessageCategory.SIGNAL_CONTACT.name
        isTranscript() -> if (isPlain) MessageCategory.PLAIN_TRANSCRIPT.name else MessageCategory.SIGNAL_TRANSCRIPT.name
        else -> if (isPlain) MessageCategory.PLAIN_TEXT.name else MessageCategory.SIGNAL_TEXT.name
    }
    return Transcript(
        messageId,
        userId,
        userFullName,
        category,
        createdAt,
        content,
        mediaUrl ?: assetUrl,
        mediaName,
        mediaSize,
        mediaWidth,
        mediaHeight,
        mediaMimeType,
        mediaDuration,
        mediaStatus,
        mediaWaveform,
        thumbImage,
        thumbUrl,
        null,
        null,
        null,
        stickerId,
        sharedUserId,
        sharedUserFullName,
        sharedUserIdentityNumber,
        sharedUserAvatarUrl,
        sharedUserAppId,
        sharedUserIsVerified,
        mentions,
        quoteId,
        quoteContent
    )
}

fun Transcript.toMessageItem(): MessageItem {
    return MessageItem(
        messageId,
        "",
        userId ?: "",
        userFullName,
        "",
        type,
        content,
        createdAt,
        "",
        MediaStatus.DONE.name,
        null,
        mediaName,
        mediaMimeType,
        mediaSize,
        thumbUrl,
        mediaWidth,
        mediaHeight,
        thumbImage,
        mediaUrl,
        mediaDuration,
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
        mediaUrl,
        null,
        null,
        null,
        stickerId,
        null,
        null,
        null,
        null,
        null,
        null,
        sharedUserId,
        sharedUserFullName,
        sharedUserIdentityNumber,
        sharedUserAvatarUrl,
        sharedUserIsVerified,
        sharedUserAppId,
        mediaWaveform,
        quoteId,
        quoteContent,
        null,
        mentions,
        null
    )
}
