package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter

@JsonClass(generateAdapter = true)
data class LiveMessagePayload(
    @Json(name = "width")
    val width: Int,
    @Json(name = "height")
    val height: Int,
    @Json(name = "thumb_url")
    val thumbUrl: String,
    @Json(name = "url")
    val url: String,
    val shareable: Boolean?
)

fun LiveMessagePayload.toJson(): String =
    getTypeAdapter<LiveMessagePayload>(LiveMessagePayload::class.java).toJson(this)
