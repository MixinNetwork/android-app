package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName
import one.mixin.android.db.web3.vo.Web3Token

data class ParsedTx(
    @SerializedName("balance_changes")
    val balanceChanges: List<BalanceChange>? = null,
    @SerializedName("instructions")
    val instructions: List<ParsedInstruction>,
    var tokens: Map<String, Web3Token>? = null,
) {
    fun noBalanceChange(): Boolean = instructions.isNotEmpty() && balanceChanges.isNullOrEmpty()
}

data class BalanceChange(
    @SerializedName("address")
    val address: String,
    @SerializedName("amount")
    val amount: Long
)

data class ParsedInstruction(
    @SerializedName("program_id")
    val programId: String,
    @SerializedName("program_name")
    val programName: String,
    @SerializedName("instruction_name")
    val instructionName: String,
    @SerializedName("items")
    val items: List<Item>? = null,
    @SerializedName("token_changes")
    val tokenChanges: List<TokenChange>? = null,
    val info: String? = null,
)

data class Item(
    @SerializedName("key")
    val key: String,
    @SerializedName("value")
    val value: String
)

data class TokenChange(
    @SerializedName("address")
    val address: String,
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("is_pay")
    val isPay: Boolean
)
