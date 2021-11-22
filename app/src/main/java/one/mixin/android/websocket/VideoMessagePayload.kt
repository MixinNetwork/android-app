package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
data class VideoMessagePayload(
    val url: String,
    @Json(name = "message_id")
    val messageId: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null,
    @Json(name = "attachment_extra")
    val attachmentExtra: String? = null,
)

fun VideoMessagePayload.toJson(): String =
    getTypeAdapter<VideoMessagePayload>(VideoMessagePayload::class.java).toJson(this)
