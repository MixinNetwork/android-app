package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class GaslessFeeRequest(
    @SerializedName("from")
    val from: String,
    @SerializedName("to")
    val to: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("chain_id")
    val chainId: String,
)