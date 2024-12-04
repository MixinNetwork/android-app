package one.mixin.android.vo.safe

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import kotlinx.parcelize.Parcelize
import one.mixin.android.api.response.web3.SwapChain
import one.mixin.android.api.response.web3.SwapToken
import one.mixin.android.api.response.web3.Swappable
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.PriceAndChange
import one.mixin.android.vo.WithdrawalMemoPossibility
import java.math.BigDecimal

@SuppressLint("ParcelCreator")
@Parcelize
data class TokenItem(
    val assetId: String,
    val symbol: String,
    val name: String,
    val iconUrl: String,
    val balance: String,
    val priceBtc: String,
    val priceUsd: String,
    val chainId: String,
    val changeUsd: String,
    val changeBtc: String,
    var hidden: Boolean?,
    val confirmations: Int,
    var chainIconUrl: String?,
    var chainSymbol: String?,
    var chainName: String?,
    var chainPriceUsd: String?,
    val assetKey: String?,
    val dust: String?,
    val withdrawalMemoPossibility: WithdrawalMemoPossibility?,
    val collectionHash: String?,
) : Parcelable, Swappable {
    fun fiat(): BigDecimal {
        return try {
            BigDecimal(balance).multiply(priceFiat())
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    }

    fun priceFiat(): BigDecimal =
        if (priceUsd == "0") {
            BigDecimal.ZERO
        } else {
            BigDecimal(priceUsd).multiply(BigDecimal(Fiats.getRate()))
        }

    fun chainPriceFiat(): BigDecimal =
        if (chainPriceUsd == null || chainPriceUsd == "0") {
            BigDecimal.ZERO
        } else {
            BigDecimal(chainPriceUsd).multiply(BigDecimal(Fiats.getRate()))
        }

    fun btc(): BigDecimal =
        if (priceBtc == "0") {
            BigDecimal.ZERO
        } else {
            BigDecimal(balance).multiply(BigDecimal(priceBtc))
        }

    fun hasDust(): Boolean {
        if (dust.isNullOrBlank()) return false

        val dustVal = dust.toDoubleOrNull() ?: return false
        return dustVal > 0
    }

    override fun toSwapToken(): SwapToken {
        return SwapToken(
            address = "",
            assetId = assetId,
            decimals = 0,
            name = name,
            symbol = symbol,
            icon = iconUrl,
            chain =
            SwapChain(
                chainId = chainId,
                decimals = 0,
                name = chainName ?: "",
                symbol = chainSymbol ?: "",
                icon = chainIconUrl ?: "",
                price = null,
            ),
            balance = balance,
            price = priceUsd,
            collectionHash = collectionHash
        )
    }

    override fun getUnique(): String {
        return assetId
    }

    companion object {
        val DIFF_CALLBACK =
            object : DiffUtil.ItemCallback<TokenItem>() {
                override fun areItemsTheSame(
                    oldItem: TokenItem,
                    newItem: TokenItem,
                ) =
                    oldItem.assetId == newItem.assetId

                override fun areContentsTheSame(
                    oldItem: TokenItem,
                    newItem: TokenItem,
                ) =
                    oldItem == newItem
            }
    }
}

fun TokenItem.toPriceAndChange(): PriceAndChange {
    return PriceAndChange(assetId, priceBtc, priceUsd, changeUsd, changeBtc)
}
