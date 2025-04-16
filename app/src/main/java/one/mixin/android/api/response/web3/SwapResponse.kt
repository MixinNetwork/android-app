package one.mixin.android.api.response.web3

data class SwapResponse(
    val tx: String?,
    val source: String,
    val displayUserId: String?,
    val depositDestination: String?,
    val quote: QuoteResult,
)
