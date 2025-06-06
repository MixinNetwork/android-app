package one.mixin.android.api.response.web3

import com.google.gson.annotations.SerializedName
import one.mixin.android.compose.theme.MixinAppTheme
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.extension.currencyFormat
import one.mixin.android.extension.numberFormat2
import one.mixin.android.vo.Fiats
import java.math.BigDecimal
import java.math.RoundingMode

data class ParsedTx(
    @SerializedName("balance_changes")
    val balanceChanges: List<BalanceChange>? = null,
    @SerializedName("instructions")
    val instructions: List<ParsedInstruction>? = null,
    @SerializedName("approves")
    val approves: List<Approve>? = null,
    val code: Int? = null
) {
    fun noBalanceChange(): Boolean = instructions?.isNotEmpty() == true && balanceChanges.isNullOrEmpty()
}

data class BalanceChange(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("asset_key")
    val address: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("decimals")
    val decimals: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("icon")
    val icon: String?,
    @SerializedName("from")
    val from: String?,
    @SerializedName("to")
    val to: String?,
) {
    fun amountString() = if ((amount.toBigDecimalOrNull()?: BigDecimal.ZERO) >= BigDecimal.ZERO) "+$amount" else amount

    fun formatPrice(priceUsd: String?): String? {
        if (priceUsd == null || priceUsd == "0") return null
        
        val amountDecimal = amount.toBigDecimalOrNull()?.abs() ?: return null
        val price = amountDecimal.multiply(BigDecimal(priceUsd)).multiply(BigDecimal(Fiats.getRate()))
        return if (price == BigDecimal.ZERO) {
            null
        } else if (price < BigDecimal("0.01")) {
            "< ${Fiats.getSymbol()} 0.01"
        } else {
            "${Fiats.getSymbol()} ${price.setScale(2, RoundingMode.HALF_UP)}"
        }
    }
}

data class Approve(
    @SerializedName("spender")
    val spender: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("icon")
    val icon: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("symbol")
    val symbol: String?,
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
