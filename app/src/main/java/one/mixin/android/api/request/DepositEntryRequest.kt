package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class DepositEntryRequest(
    @SerializedName("chain_id")
    val chainId: String,
    @SerializedName("asset_id")
    val assetId: String?,
)
