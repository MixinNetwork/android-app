package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName

data class QuoteResponse(
    @SerializedName("input_mint") val inputMint: String,
    @SerializedName("in_amount") val inAmount: String,
    @SerializedName("output_mint") val outputMint: String,
    @SerializedName("out_amount") val outAmount: String,
    @SerializedName("other_amount_threshold") val otherAmountThreshold: String,
    @SerializedName("swap_mode") val swapMode: String,
    @SerializedName("slippage_bps") val slippageBps: Int,
    @SerializedName("price_impact_pct") val priceImpactPct: String,
    @SerializedName("route_plan") val routePlan: List<RoutePlan>,
    @SerializedName("platform_fee") val platformFee: PlatformFee? = null,
    @SerializedName("context_slot") val contextSlot: Float? = null,
    @SerializedName("time_taken") val timeTaken: Float? = null
)

data class PlatformFee(
    @SerializedName("amount") val amount: String? = null,
    @SerializedName("fee_bps") val feeBps: Int? = null
)

data class RoutePlan(
    @SerializedName("swap_info") val swapInfo: SwapInfo,
    @SerializedName("percent") val percent: Int
)

data class SwapInfo(
    @SerializedName("amm_key") val ammKey: String,
    @SerializedName("label") val label: String? = null,
    @SerializedName("input_mint") val inputMint: String,
    @SerializedName("output_mint") val outputMint: String,
    @SerializedName("in_amount") val inAmount: String,
    @SerializedName("out_amount") val outAmount: String,
    @SerializedName("fee_amount") val feeAmount: String,
    @SerializedName("fee_mint") val feeMint: String
)