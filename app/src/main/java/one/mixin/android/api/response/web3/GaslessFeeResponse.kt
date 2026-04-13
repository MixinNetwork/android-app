package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class GaslessFeeResponse(
    val fees: List<GaslessFeeEstimate>,
)

data class GaslessFeeEstimate(
    @SerializedName("asset_id")
    val assetId: String,
    val amount: String,
)