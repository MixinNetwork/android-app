package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class AssetFee(
    val type: String,
    @SerializedName("asset_id")
    val assetId: String,
    val amount: String,
)
