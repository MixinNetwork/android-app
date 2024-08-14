package one.mixin.android.api.request.web3

data class StakeRequest(
    val chain: String = "solana",
    val payer: String,
    val amount: String,
    val action: String,
    val vote: String? = null, // for stake
    val pubkey: String? = null, // for unstake and withdraw
)

@Suppress("EnumEntryName")
enum class StakeAction {
    delegate, deactive, withdraw
}