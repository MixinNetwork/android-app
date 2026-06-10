package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class GaslessFeeRequest(
    val from: String,
    val to: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("chain_id")
    val chainId: String,
)