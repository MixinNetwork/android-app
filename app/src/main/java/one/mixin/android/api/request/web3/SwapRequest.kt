package one.mixin.android.api.request.web3

import one.mixin.android.api.response.web3.JupiterQuoteResponse

data class SwapRequest(
    val payer: String,
    val inputMint: String,
    val inputAmount: String,
    val outputMint: String,
    val outputAmount: String,
    val inputChainId: Int,
    val outputChainId: Int,
    val slippage: Int,
    val source: String,

    val jupiterQuoteResponse: JupiterQuoteResponse? = null,
)
