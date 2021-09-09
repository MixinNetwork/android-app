package one.mixin.android.vo

import android.content.Context
import com.google.gson.annotations.SerializedName
import one.mixin.android.R

class PinMessageMinimal(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("category")
    override val type: String,
    val content: String?
) : ICategory

fun PinMessageMinimal.explain(context: Context): CharSequence {
    return when {
        isImage() -> context.getText(R.string.chat_pin_image_message)
        isVideo() -> context.getText(R.string.chat_pin_video_message)
        isLive() -> context.getText(R.string.chat_pin_live_message)
        isData() -> context.getText(R.string.chat_pin_data_message)
        isAudio() -> context.getText(R.string.chat_pin_audio_message)
        isSticker() -> context.getText(R.string.chat_pin_sticker_message)
        isContact() -> context.getText(R.string.chat_pin_contact_message)
        isPost() -> context.getText(R.string.chat_pin_post_message)
        isLocation() -> context.getText(R.string.chat_pin_location_message)
        isTranscript() -> context.getText(R.string.chat_pin_transcript_message)
        isAppCard() -> context.getText(R.string.chat_pin_card_message)
        else -> " \"${content}\""
    }
}
