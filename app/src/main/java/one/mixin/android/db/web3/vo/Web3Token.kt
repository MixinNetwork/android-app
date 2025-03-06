package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Swappable
import one.mixin.android.vo.Fiats
import one.mixin.android.web3.Web3ChainId
import org.sol4k.VersionedTransaction
import org.sol4k.lamportToSol
import java.math.BigDecimal
import java.math.RoundingMode

@Entity(
    tableName = "tokens",
    primaryKeys = ["wallet_id", "asset_id"]
)
@Parcelize
data class Web3Token(
    @ColumnInfo(name = "wallet_id")
    @SerializedName("wallet_id")
    val walletId: String,
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    val chainId: String,
    @ColumnInfo(name = "name")
    @SerializedName("name")
    val name: String,
    @ColumnInfo(name = "asset_key")
    @SerializedName("asset_key")
    val assetKey: String,
    @ColumnInfo(name = "symbol")
    @SerializedName("symbol")
    val symbol: String,
    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "precision")
    @SerializedName("precision")
    val precision: Int,
    @ColumnInfo(name = "kernel_asset_id")
    @SerializedName("kernel_asset_id")
    val kernelAssetId: String = "",
    @ColumnInfo(name = "amount")
    @SerializedName("amount")
    val balance: String,
    @ColumnInfo(name = "price_usd")
    @SerializedName("price_usd")
    val priceUsd: String,
    @ColumnInfo(name = "change_usd")
    @SerializedName("change_usd")
    val changeUsd: String,
) : Parcelable, Swappable {

    override fun toSwapToken(): SwapToken {
        return SwapToken(
            address = if (assetKey == solanaNativeTokenAssetKey) wrappedSolTokenAssetKey else assetKey,
            assetId = assetId,
            decimals = precision,
            name = name,
            symbol = symbol,
            icon = iconUrl,
            chain =
            SwapChain(
                chainId = chainId,
                decimals = precision,
                name = "",
                symbol = symbol,
                icon = "",
                price = null,
            ),
            balance = balance,
            price = priceUsd,
        )
    }

    override fun getUnique(): String {
        return if (assetKey == solanaNativeTokenAssetKey) wrappedSolTokenAssetKey else assetKey
    }

    fun fiat(): BigDecimal = try {
        BigDecimal(balance).multiply(priceFiat())
    } catch (e: NumberFormatException) {
        BigDecimal.ZERO
    }

    fun priceFiat(): BigDecimal = when (priceUsd) {
        "0" -> BigDecimal.ZERO
        "" -> BigDecimal.ZERO
        else -> BigDecimal(priceUsd).multiply(Fiats.getRate().toBigDecimal())
    }

    fun toStringAmount(amount: Long): String {
        return realAmount(amount).stripTrailingZeros().toPlainString()
    }

    fun realAmount(amount: Long): BigDecimal {
        return BigDecimal(amount).divide(BigDecimal.TEN.pow(precision)).setScale(9, RoundingMode.CEILING)
    }
}

fun Web3TokenItem.isSolToken(): Boolean {
    return isSolana() && (assetKey == solanaNativeTokenAssetKey || assetKey == wrappedSolTokenAssetKey)
}

private fun Web3Token.getChainAssetKey(): String {
    return if (chainId.equals("ethereum", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("base", true)) {
        "0x0000000000000000000000000000000000000000"}
    else if (chainId.equals("blast", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("arbitrum", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("optimism", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("polygon", true)) {
        "0x0000000000000000000000000000000000001010"
    } else if (chainId.equals("binance-smart-chain", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("avalanche", true)) {
        "0x0000000000000000000000000000000000000000"
    } else if (chainId.equals("solana", true)) {
        solanaNativeTokenAssetKey
    } else {
        ""
    }
}

fun Web3TokenItem.calcSolBalanceChange(balanceChange: VersionedTransaction.TokenBalanceChange): String {
    return if (isSolToken()) {
        lamportToSol(BigDecimal(balanceChange.change))
    } else {
        BigDecimal(balanceChange.change).divide(BigDecimal.TEN.pow(precision)).setScale(precision, RoundingMode.CEILING)
    }.stripTrailingZeros().toPlainString()
}

fun Long.solLamportToAmount(scale: Int = 9): BigDecimal {
    return BigDecimal(this).divide(BigDecimal.TEN.pow(9)).setScale(scale, RoundingMode.CEILING)
}
