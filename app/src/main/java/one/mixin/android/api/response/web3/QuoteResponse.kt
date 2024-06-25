package one.mixin.android.api.response.web3

data class QuoteResponse(
    val inputMint: String,
    val inAmount: String,
    val outputMint: String,
    val outAmount: String,
    val slippage: Int,
    val source: String,
    val jupiterQuoteResponse: JupiterQuoteResponse? = null,
)