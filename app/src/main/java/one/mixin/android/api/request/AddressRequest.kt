package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AddressRequest(
    @Json(name = "asset_id")
    val assetId: String,
    @Json(name = "destination")
    val destination: String?,
    @Json(name = "tag")
    val tag: String?,
    val label: String?,
    val pin: String
)
