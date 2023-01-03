package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

data class AssetPrecision(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("chain_id")
    val chainId: String,
    val precision: Int,
)
