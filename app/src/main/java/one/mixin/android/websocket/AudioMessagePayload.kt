package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class AudioMessagePayload(
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("duration")
    val duration: Long,
    @SerializedName("wave_form")
    val waveForm: ByteArray,
    @SerializedName("attachment_extra")
    val attachmentExtra: String? = null,
    @SerializedName("shareable")
    val shareable: Boolean? = null,
)
