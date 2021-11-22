package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AssetFee(
    val type: String,
    @Json(name = "asset_id")
    val assetId: String,
    val amount: String
)
