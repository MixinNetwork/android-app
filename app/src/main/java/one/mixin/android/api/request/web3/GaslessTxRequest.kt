package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class GaslessTxRequest(
    @SerializedName("from")
    val from: String,
    @SerializedName("to")
    val to: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("fee_asset_id")
    val feeAssetId: String,
    @SerializedName("fee_amount")
    val feeAmount: String?,
    @SerializedName("chain_id")
    val chainId: String,
)
