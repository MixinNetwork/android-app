package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.util.MoshiHelper

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
    MoshiHelper.getTypeAdapter<LiveMessagePayload>(LiveMessagePayload::class.java).toJson(this)
