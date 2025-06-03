package one.mixin.android.api.request.web3

data class SwapRequest(
    val payer: String,
    val inputMint: String,
    val inputAmount: String,
    val outputMint: String,
    val payload: String,
    val source: String,
    val withdrawalDestination: String?,
    val referral: String?,
)
