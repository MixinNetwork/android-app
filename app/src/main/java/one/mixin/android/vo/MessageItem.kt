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
import java.io.File
import java.io.FileInputStream
import kotlinx.android.parcel.Parcelize
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.hasWritePermission
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.toast
import one.mixin.android.util.VideoPlayer

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
    val type: String,
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
) : Parcelable {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MessageItem>() {
            override fun areItemsTheSame(oldItem: MessageItem, newItem: MessageItem) =
                oldItem.messageId == newItem.messageId

            override fun areContentsTheSame(oldItem: MessageItem, newItem: MessageItem) =
                oldItem == newItem
        }
    }
}

fun create(type: String, createdAt: String? = null) = MessageItem(
    "", "", "", "", "",
    type, null, createdAt
        ?: nowInUtc(), MessageStatus.READ.name, null, null,
    null, null, null, null, null, null, null, null,
    null, null, null, null, null, null, null, null,
    null, null, null, null, null, null, null, null, null, null
)

fun MessageItem.isMedia(): Boolean = this.type == MessageCategory.SIGNAL_IMAGE.name ||
    this.type == MessageCategory.PLAIN_IMAGE.name ||
    this.type == MessageCategory.SIGNAL_DATA.name ||
    this.type == MessageCategory.PLAIN_DATA.name ||
    this.type == MessageCategory.SIGNAL_VIDEO.name ||
    this.type == MessageCategory.PLAIN_VIDEO.name

fun MessageItem.canNotForward() = this.type == MessageCategory.APP_BUTTON_GROUP.name ||
    this.type == MessageCategory.SYSTEM_ACCOUNT_SNAPSHOT.name ||
    this.type == MessageCategory.SYSTEM_CONVERSATION.name ||
    (!mediaDownloaded(this.mediaStatus) && this.isMedia()) ||
    isCallMessage() || isRecall()

fun MessageItem.supportSticker(): Boolean = this.type == MessageCategory.SIGNAL_STICKER.name ||
    this.type == MessageCategory.PLAIN_STICKER.name ||
    this.type == MessageCategory.SIGNAL_IMAGE.name ||
    this.type == MessageCategory.PLAIN_IMAGE.name

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

fun MessageItem.isLive() = type == MessageCategory.PLAIN_LIVE.name || type == MessageCategory.SIGNAL_LIVE.name

fun MessageItem.isImage() = type == MessageCategory.PLAIN_IMAGE.name || type == MessageCategory.SIGNAL_IMAGE.name

fun MessageItem.isVideo() = type == MessageCategory.SIGNAL_VIDEO.name || type == MessageCategory.PLAIN_VIDEO.name

fun MessageItem.isPost() = type == MessageCategory.SIGNAL_POST.name || type == MessageCategory.SIGNAL_POST.name

fun MessageItem.isAudio() =
    type == MessageCategory.PLAIN_AUDIO.name ||
        type == MessageCategory.SIGNAL_AUDIO.name

fun MessageItem.isFile() =
    type == MessageCategory.SIGNAL_DATA.name ||
        type == MessageCategory.PLAIN_DATA.name

fun MessageItem.mediaDownloaded() = mediaStatus == MediaStatus.DONE.name || mediaStatus == MediaStatus.READ.name

fun MessageItem.canRecall(): Boolean {
    return this.type == MessageCategory.SIGNAL_TEXT.name ||
        this.type == MessageCategory.SIGNAL_IMAGE.name ||
        this.type == MessageCategory.SIGNAL_VIDEO.name ||
        this.type == MessageCategory.SIGNAL_STICKER.name ||
        this.type == MessageCategory.SIGNAL_DATA.name ||
        this.type == MessageCategory.SIGNAL_CONTACT.name ||
        this.type == MessageCategory.SIGNAL_AUDIO.name ||
        this.type == MessageCategory.SIGNAL_LIVE.name ||
        this.type == MessageCategory.PLAIN_TEXT.name ||
        this.type == MessageCategory.PLAIN_IMAGE.name ||
        this.type == MessageCategory.PLAIN_VIDEO.name ||
        this.type == MessageCategory.PLAIN_STICKER.name ||
        this.type == MessageCategory.PLAIN_DATA.name ||
        this.type == MessageCategory.PLAIN_CONTACT.name ||
        this.type == MessageCategory.PLAIN_AUDIO.name ||
        this.type == MessageCategory.PLAIN_LIVE.name
}

fun MessageItem.isRecall() = type == MessageCategory.MESSAGE_RECALL.name

fun MessageItem.isSignal() = type.startsWith("SIGNAL_")

fun MessageItem.toMessage() = Message(
    messageId, conversationId, userId, type, content, mediaUrl, mediaMimeType, mediaSize,
    mediaDuration, mediaWidth, mediaHeight, null, thumbImage, thumbUrl, null, null, mediaStatus, status,
    createdAt, actionName, participantUserId, snapshotId, hyperlink = null, name = mediaName, albumId = null, stickerId = stickerId,
    sharedUserId = sharedUserId, mediaWaveform = mediaWaveform, mediaMineType = null, quoteMessageId = quoteId, quoteContent = quoteContent
)

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
