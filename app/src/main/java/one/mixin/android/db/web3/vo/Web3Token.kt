package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.Constants
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Swappable
import one.mixin.android.vo.Fiats
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
    @ColumnInfo(name = "level")
    @SerializedName("level")
    val level: Int = Constants.AssetLevel.VERIFIED,
) : Parcelable, Swappable {

    override fun toSwapToken(): SwapToken {
        return SwapToken(
            walletId = walletId,
            address = assetKey,
            assetId = assetId,
            decimals = precision,
            name = name,
            symbol = symbol,
            icon = iconUrl,
            chain =
            SwapChain(
                chainId = chainId,
                name = "",
                symbol = symbol,
                icon = "",
                price = null,
            ),
            balance = balance,
            price = priceUsd,
            isWeb3 = true
        )
    }

    override fun getUnique(): String {
        return assetId
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

    fun isNotVerified() = level < Constants.AssetLevel.VERIFIED
}

fun Web3TokenItem.isNativeSolToken(): Boolean {
    return isSolanaChain() && assetId == Constants.ChainId.SOLANA_CHAIN_ID
}
fun Long.solLamportToAmount(scale: Int = 9): BigDecimal {
    return BigDecimal(this).divide(BigDecimal.TEN.pow(9)).setScale(scale, RoundingMode.CEILING)
}
