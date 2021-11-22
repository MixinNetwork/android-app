package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StickerAddRequest(
    @Json(name ="data_base64")
    val dataBase64: String? = null,
    @Json(name ="sticker_id")
    val stickerId: String? = null
)
