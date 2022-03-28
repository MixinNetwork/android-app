package one.mixin.android.vo

import android.content.Context
import androidx.core.net.toUri
import one.mixin.android.extension.generateConversationPath
import one.mixin.android.extension.getAudioPath
import one.mixin.android.extension.getDocumentPath
import one.mixin.android.extension.getImagePath
import one.mixin.android.extension.getTranscriptDirPath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.isFileUri
import java.io.File

interface ICategory {
    val type: String?
}

fun ICategory.isPlain(): Boolean {
    return type?.startsWith("PLAIN_") == true
}

fun ICategory.isEncrypted(): Boolean {
    return type?.startsWith("ENCRYPTED_") == true
}

fun ICategory.isSignal(): Boolean {
    return type?.startsWith("SIGNAL_") == true
}

fun ICategory.isSecret(): Boolean {
    return isSignal() || isEncrypted()
}

fun ICategory.isCall() = type?.startsWith("WEBRTC_") == true || type?.startsWith("KRAKEN_") == true

fun ICategory.isKraken() = type?.startsWith("KRAKEN_") == true

fun ICategory.isRecall() = type == MessageCategory.MESSAGE_RECALL.name

fun ICategory.isPin() = type == MessageCategory.MESSAGE_PIN.name

fun ICategory.isFtsMessage() =
    type?.endsWith("_TEXT") == true || type?.endsWith("_DATA") == true || type?.endsWith("_POST") == true || type?.endsWith("_TRANSCRIPT") == true

fun ICategory.isText() =
    type == MessageCategory.SIGNAL_TEXT.name || type == MessageCategory.PLAIN_TEXT.name || type == MessageCategory.ENCRYPTED_TEXT.name

fun ICategory.isLive() =
    type == MessageCategory.SIGNAL_LIVE.name || type == MessageCategory.PLAIN_LIVE.name || type == MessageCategory.ENCRYPTED_LIVE.name

fun ICategory.isImage() =
    type == MessageCategory.SIGNAL_IMAGE.name || type == MessageCategory.PLAIN_IMAGE.name || type == MessageCategory.ENCRYPTED_IMAGE.name

fun ICategory.isVideo() =
    type == MessageCategory.SIGNAL_VIDEO.name || type == MessageCategory.PLAIN_VIDEO.name || type == MessageCategory.ENCRYPTED_VIDEO.name

fun ICategory.isSticker() =
    type == MessageCategory.SIGNAL_STICKER.name || type == MessageCategory.PLAIN_STICKER.name || type == MessageCategory.ENCRYPTED_STICKER.name

fun ICategory.isPost() =
    type == MessageCategory.SIGNAL_POST.name || type == MessageCategory.PLAIN_POST.name || type == MessageCategory.ENCRYPTED_POST.name

fun ICategory.isAudio() =
    type == MessageCategory.SIGNAL_AUDIO.name || type == MessageCategory.PLAIN_AUDIO.name || type == MessageCategory.ENCRYPTED_AUDIO.name

fun ICategory.isData() =
    type == MessageCategory.SIGNAL_DATA.name || type == MessageCategory.PLAIN_DATA.name || type == MessageCategory.ENCRYPTED_DATA.name

fun ICategory.isLocation() =
    type == MessageCategory.SIGNAL_LOCATION.name || type == MessageCategory.PLAIN_LOCATION.name || type == MessageCategory.ENCRYPTED_LOCATION.name

fun ICategory.isContact() =
    type == MessageCategory.SIGNAL_CONTACT.name || type == MessageCategory.PLAIN_CONTACT.name || type == MessageCategory.ENCRYPTED_CONTACT.name

fun ICategory.isMedia(): Boolean = isData() || isImage() || isVideo()

fun ICategory.isAttachment(): Boolean = isData() || isImage() || isVideo() || isAudio()

fun ICategory.isGroupCall() = type?.isGroupCallType() == true

fun ICategory.isTranscript() = type == MessageCategory.PLAIN_TRANSCRIPT.name || type == MessageCategory.SIGNAL_TRANSCRIPT.name || type == MessageCategory.ENCRYPTED_TRANSCRIPT.name

fun ICategory.isAppCard() = type == MessageCategory.APP_CARD.name

fun ICategory.isAppButtonGroup() = type == MessageCategory.APP_BUTTON_GROUP.name

fun ICategory.isCallMessage() =
    type == MessageCategory.WEBRTC_AUDIO_CANCEL.name ||
        type == MessageCategory.WEBRTC_AUDIO_DECLINE.name ||
        type == MessageCategory.WEBRTC_AUDIO_END.name ||
        type == MessageCategory.WEBRTC_AUDIO_BUSY.name ||
        type == MessageCategory.WEBRTC_AUDIO_FAILED.name

fun ICategory.canRecall(): Boolean {
    return type == MessageCategory.SIGNAL_TEXT.name ||
        type == MessageCategory.SIGNAL_IMAGE.name ||
        type == MessageCategory.SIGNAL_VIDEO.name ||
        type == MessageCategory.SIGNAL_STICKER.name ||
        type == MessageCategory.SIGNAL_DATA.name ||
        type == MessageCategory.SIGNAL_CONTACT.name ||
        type == MessageCategory.SIGNAL_AUDIO.name ||
        type == MessageCategory.SIGNAL_LIVE.name ||
        type == MessageCategory.SIGNAL_POST.name ||
        type == MessageCategory.SIGNAL_LOCATION.name ||
        type == MessageCategory.SIGNAL_TRANSCRIPT.name ||
        type == MessageCategory.PLAIN_TEXT.name ||
        type == MessageCategory.PLAIN_IMAGE.name ||
        type == MessageCategory.PLAIN_VIDEO.name ||
        type == MessageCategory.PLAIN_STICKER.name ||
        type == MessageCategory.PLAIN_DATA.name ||
        type == MessageCategory.PLAIN_CONTACT.name ||
        type == MessageCategory.PLAIN_AUDIO.name ||
        type == MessageCategory.PLAIN_LIVE.name ||
        type == MessageCategory.PLAIN_POST.name ||
        type == MessageCategory.PLAIN_LOCATION.name ||
        type == MessageCategory.PLAIN_TRANSCRIPT.name ||
        type == MessageCategory.APP_CARD.name ||
        type == MessageCategory.ENCRYPTED_TEXT.name ||
        type == MessageCategory.ENCRYPTED_IMAGE.name ||
        type == MessageCategory.ENCRYPTED_VIDEO.name ||
        type == MessageCategory.ENCRYPTED_STICKER.name ||
        type == MessageCategory.ENCRYPTED_DATA.name ||
        type == MessageCategory.ENCRYPTED_CONTACT.name ||
        type == MessageCategory.ENCRYPTED_AUDIO.name ||
        type == MessageCategory.ENCRYPTED_LIVE.name ||
        type == MessageCategory.ENCRYPTED_POST.name ||
        type == MessageCategory.ENCRYPTED_LOCATION.name ||
        type == MessageCategory.ENCRYPTED_TRANSCRIPT.name
}

fun ICategory.absolutePath(context: Context, conversationId: String, mediaUrl: String?): String? {
    if (mediaUrl == null) return null
    if (isLive()) {
        return mediaUrl
    }
    if (mediaUrl.isFileUri()) {
        return mediaUrl.toUri().toString()
    }
    if (conversationId.isBlank()) {
        return File(context.getTranscriptDirPath(), mediaUrl).toUri().toString()
    }
    return generatePath(context, false, this, conversationId, mediaUrl)?.run {
        if (this.exists()) return@run this
        else return@run generatePath(context, true, this@absolutePath, conversationId, mediaUrl)
    }?.toUri().toString()
}

private fun generatePath(
    context: Context,
    legacy: Boolean,
    iCategory: ICategory,
    conversationId: String,
    mediaUrl: String
): File? {
    return when {
        iCategory.isImage() -> File(
            context.getImagePath(legacy).generateConversationPath(conversationId), mediaUrl
        )
        iCategory.isVideo() -> File(
            context.getVideoPath(legacy).generateConversationPath(conversationId), mediaUrl
        )
        iCategory.isAudio() -> File(
            context.getAudioPath(legacy).generateConversationPath(conversationId), mediaUrl
        )
        iCategory.isData() -> File(
            context.getDocumentPath(legacy).generateConversationPath(conversationId),
            mediaUrl
        )
        iCategory.isTranscript() -> File(context.getTranscriptDirPath(legacy), mediaUrl)
        else -> null
    }
}
