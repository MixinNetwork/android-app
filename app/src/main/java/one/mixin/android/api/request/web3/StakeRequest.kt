package one.mixin.android.api.request.web3

import com.google.gson.annotations.SerializedName

data class StakeRequest(
    @SerializedName("chain")
    val chain: String = "solana",
    @SerializedName("payer")
    val payer: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("action")
    val action: String,
    @SerializedName("vote")
    val vote: String? = null, // for stake
    @SerializedName("pubkey")
    val pubkey: String? = null, // for unstake and withdraw
)

@Suppress("EnumEntryName")
enum class StakeAction {
    delegate, deactive, withdraw
}