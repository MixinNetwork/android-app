package one.mixin.android.vo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.google.android.exoplayer2.util.MimeTypes
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.generateConversationPath
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getMediaPath
import one.mixin.android.extension.getPublicPicturePath
import one.mixin.android.extension.getTranscriptDirPath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.hasWritePermission
import one.mixin.android.extension.isImageSupport
import one.mixin.android.extension.toast
import one.mixin.android.util.VideoPlayer
import java.io.File
import java.io.FileInputStream

class ChatHistoryMessageItem(
    val transcriptId: String? = null,
    val conversationId: String? = null,
    val messageId: String,
    val userId: String?,
    val userFullName: String?,
    val userIdentityNumber: String?,
    override val type: String,
    val appId: String?,
    val content: String?,
    val createdAt: String,
    val mediaStatus: String?,
    val mediaName: String?,
    val mediaMimeType: String?,
    val mediaSize: Long?,
    val thumbUrl: String?,
    val mediaWidth: Int?,
    val mediaHeight: Int?,
    val thumbImage: String?,
    val mediaUrl: String?,
    val mediaDuration: String?,
    val mediaWaveform: ByteArray? = null,
    val assetWidth: Int? = null,
    val assetHeight: Int? = null,
    val assetUrl: String? = null,
    val assetType: String? = null,
    val sharedUserId: String? = null,
    val sharedUserFullName: String? = null,
    val sharedUserAvatarUrl: String? = null,
    val sharedUserIdentityNumber: String? = null,
    val sharedUserIsVerified: Boolean? = null,
    val sharedUserAppId: String? = null,
    val quoteId: String? = null,
    val quoteContent: String? = null,
    val mentions: String? = null,
) : ICategory

fun ChatHistoryMessageItem.isLottie() = assetType?.equals(Sticker.STICKER_TYPE_JSON, true) == true

fun ChatHistoryMessageItem.showVerifiedOrBot(verifiedView: View, botView: View) {
    when {
        sharedUserIsVerified == true -> {
            verifiedView.isVisible = true
            botView.isVisible = false
        }
        appId != null -> {
            verifiedView.isVisible = false
            botView.isVisible = true
        }
        else -> {
            verifiedView.isVisible = false
            botView.isVisible = false
        }
    }
}

fun ChatHistoryMessageItem.saveToLocal(context: Context) {
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

fun ChatHistoryMessageItem.loadVideoOrLive(actionAfterLoad: (() -> Unit)? = null) {
    mediaUrl?.let {
        if (isLive()) {
            VideoPlayer.player().loadHlsVideo(it, messageId)
        } else {
            VideoPlayer.player().loadVideo(it, messageId)
        }
        actionAfterLoad?.invoke()
    }
}

fun ChatHistoryMessageItem.toMessageItem(conversationId: String?): MessageItem {
    return MessageItem(
        messageId,
        conversationId ?: "",
        userId ?: "", userFullName ?: "",
        userIdentityNumber ?: "",
        type,
        content,
        createdAt,
        MessageStatus.DELIVERED.name,
        mediaStatus,
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
        assetType,
        null,
        null,
        assetUrl,
        assetHeight,
        assetWidth,
        null,
        null,
        null,
        appId,
        null, null, null, null, null, null, null, null,
        sharedUserIsVerified,
        sharedUserAppId,
        mediaWaveform,
        quoteId,
        quoteContent,
        null,
        mentions = null,
        null
    )
}

private val mediaPath by lazy {
    MixinApplication.appContext.getMediaPath()?.toUri()?.toString()
}

fun ChatHistoryMessageItem.absolutePath(context: Context = MixinApplication.appContext): String? {
    if (transcriptId == null && conversationId != null) {
        val dirPath = mediaPath ?: return null
        return when {
            mediaUrl == null -> null
            mediaUrl.startsWith(dirPath) -> mediaUrl
            isImage() -> File(context.getImagePath().generateConversationPath(conversationId), mediaUrl).toUri().toString()
            isVideo() -> File(context.getVideoPath().generateConversationPath(conversationId), mediaUrl).toUri().toString()
            isAudio() -> File(context.getAudioPath().generateConversationPath(conversationId), mediaUrl).toUri().toString()
            isData() -> File(context.getDocumentPath().generateConversationPath(conversationId), mediaUrl).toUri().toString()
            isTranscript() -> File(context.getTranscriptDirPath(), mediaUrl).toUri().toString()
            else -> null
        }
    }
    val mediaPath = MixinApplication.appContext.getMediaPath()?.toUri()?.toString() ?: return null
    val url = mediaUrl
    return when {
        url == null -> null
        isLive() -> url
        url.startsWith(mediaPath) -> url
        else -> File(context.getTranscriptDirPath(), url).toUri().toString()
    }
}
