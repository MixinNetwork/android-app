package one.mixin.android.api.response.web3

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class QuoteResponse(
    @SerializedName("inputMint") val inputMint: String,
    @SerializedName("inAmount") val inAmount: String,
    @SerializedName("outputMint") val outputMint: String,
    @SerializedName("outAmount") val outAmount: String,
    @SerializedName("otherAmountThreshold") val otherAmountThreshold: String,
    @SerializedName("swapMode") val swapMode: String,
    @SerializedName("slippageBps") val slippageBps: Int,
    @SerializedName("priceImpactPct") val priceImpactPct: String,
    @SerializedName("routePlan") val routePlan: List<RoutePlan>,
    @SerializedName("platformFee") val platformFee: PlatformFee? = null,
    @SerializedName("contextSlot") val contextSlot: Float? = null,
    @SerializedName("timeTaken") val timeTaken: Float? = null
) : Parcelable

@Parcelize
data class PlatformFee(
    @SerializedName("amount") val amount: String? = null,
    @SerializedName("feeBps") val feeBps: Int? = null
) : Parcelable

@Parcelize
data class RoutePlan(
    @SerializedName("swapInfo") val swapInfo: SwapInfo,
    @SerializedName("percent") val percent: Int
) : Parcelable

@Parcelize
data class SwapInfo(
    @SerializedName("ammKey") val ammKey: String,
    @SerializedName("label") val label: String? = null,
    @SerializedName("inputMint") val inputMint: String,
    @SerializedName("outputMint") val outputMint: String,
    @SerializedName("inAmount") val inAmount: String,
    @SerializedName("outAmount") val outAmount: String,
    @SerializedName("feeAmount") val feeAmount: String,
    @SerializedName("feeMint") val feeMint: String
) : Parcelable
