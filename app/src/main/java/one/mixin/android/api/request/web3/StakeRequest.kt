package one.mixin.android.api.request.web3

data class StakeRequest(
    val payer: String,
    val amount: Long,
    val action: StakeAction,
    val vote: String? = null,
)

enum class StakeAction {
    Delegate, Deactive, Withdraw
}