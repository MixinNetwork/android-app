package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class AudioMessagePayload(
    @SerializedName("message_id")
    val messageId: String,
    val url: String,
    val duration: Long,
    @SerializedName("wave_form")
    val waveForm: ByteArray,
    @SerializedName("attachment_content")
    val attachmentContent: String? = null,
)
