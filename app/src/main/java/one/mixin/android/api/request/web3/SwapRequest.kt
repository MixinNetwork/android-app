package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class SwapRequest(
    @SerializedName("payer")
    val payer: String,
    @SerializedName("inputMint")
    val inputMint: String,
    @SerializedName("inputAmount")
    val inputAmount: String,
    @SerializedName("outputMint")
    val outputMint: String,
    @SerializedName("payload")
    val payload: String,
    @SerializedName("source")
    val source: String,
    @SerializedName("withdrawalDestination")
    val withdrawalDestination: String?,
    @SerializedName("referral")
    val referral: String?,
    @SerializedName("walletId")
    val walletId: String?,
)
