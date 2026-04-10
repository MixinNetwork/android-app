package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class GaslessTxRequest(
    val from: String,
    val to: String,
    @SerializedName("asset_id")
    val assetId: String,
    val amount: String,
    @SerializedName("fee_asset_id")
    val feeAssetId: String,
    @SerializedName("chain_id")
    val chainId: String,
)