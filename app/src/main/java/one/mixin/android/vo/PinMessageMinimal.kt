package one.mixin.android.vo

import android.content.Context
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.R
import one.mixin.android.util.MoshiHelper

@JsonClass(generateAdapter = true)
class PinMessageMinimal(
    @SerializedName("message_id")
    @Json(name = "message_id")
    val messageId: String,
    @SerializedName("category")
    @Json(name = "category")
    override val type: String,
    @Json(name = "content")
    val content: String?
) : ICategory

fun PinMessageMinimal.toJson(): String? = MoshiHelper.getTypeAdapter<PinMessageMinimal>(PinMessageMinimal::class.java).toJson(this)

fun PinMessageMinimal?.explain(context: Context): CharSequence {
    return when {
        this == null -> context.getText(R.string.chat_pin_empty_message)
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
        isAppButtonGroup() -> context.getText(R.string.chat_pin_empty_message)
        else -> " \"${content}\""
    }
}
