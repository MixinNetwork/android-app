package one.mixin.android.websocket

import com.google.gson.annotations.SerializedName
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

data class VideoMessagePayload(
    val url: String,
    @SerializedName("message_id")
    val messageId: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("attachment_extra")
    val attachmentExtra: String? = null,
)

fun VideoMessagePayload.toJson(): String =
    getTypeAdapter<VideoMessagePayload>(VideoMessagePayload::class.java).toJson(this)
