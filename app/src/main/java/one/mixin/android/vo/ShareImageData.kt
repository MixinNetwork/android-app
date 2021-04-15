package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

data class ShareImageData(
    val url: String,
    @SerializedName("attachment_message_payload")
    val attachmentMessagePayload: String? = null,
)
