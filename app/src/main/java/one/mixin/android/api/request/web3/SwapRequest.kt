package one.mixin.android.api.request.web3

import one.mixin.android.api.response.web3.JupiterQuoteResponse

data class SwapRequest(
    val payer: String,
    val inputMint: String,
    val inAmount: Long,
    val inputAmount: String, // exin
    val outputMint: String,
    val slippage: Int,
    val source: String,
    val payload: String,

    val jupiterQuoteResponse: JupiterQuoteResponse? = null,
)
