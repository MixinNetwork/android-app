package one.mixin.android.websocket

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.util.MoshiHelper

@JsonClass(generateAdapter = true)
data class StickerMessagePayload(
    @Json(name = "sticker_id")
    val stickerId: String? = null,
    @Json(name = "album_id")
    val albumId: String? = null,
    @Json(name = "name")
    val name: String? = null
)

fun StickerMessagePayload.toJson(): String =
    MoshiHelper.getTypeAdapter<StickerMessagePayload>(StickerMessagePayload::class.java).toJson(this)
