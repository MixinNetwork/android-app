package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName

data class VideoMessagePayload(
    val url: String,
    @SerializedName("message_id")
    val messageId: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
)
