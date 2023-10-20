package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import kotlinx.parcelize.Parcelize
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
    val reserve: String?,
    val withdrawalMemoPossibility: WithdrawalMemoPossibility?,
) : Parcelable {
    fun fiat(): BigDecimal {
        return try {
            BigDecimal(balance).multiply(priceFiat())
        } catch (e: NumberFormatException) {
            BigDecimal.ZERO
        }
    }

    fun priceFiat(): BigDecimal = if (priceUsd == "0") {
        BigDecimal.ZERO
    } else {
        BigDecimal(priceUsd).multiply(BigDecimal(Fiats.getRate()))
    }

    fun chainPriceFiat(): BigDecimal = if (chainPriceUsd == null || chainPriceUsd == "0") {
        BigDecimal.ZERO
    } else {
        BigDecimal(chainPriceUsd).multiply(BigDecimal(Fiats.getRate()))
    }

    fun btc(): BigDecimal = if (priceBtc == "0") {
        BigDecimal.ZERO
    } else {
        BigDecimal(balance).multiply(BigDecimal(priceBtc))
    }

    fun getDestination(): String {
        return ""
        // return if (assetId == BITCOIN_CHAIN_ID) {
        //     depositEntries?.firstOrNull { depositEntry ->
        //         depositEntry.properties != null && depositEntry.destination.isNotBlank() && depositEntry.properties.any { property ->
        //             property.equals(
        //                 "SegWit",
        //                 false,
        //             )
        //         }
        //     }?.destination ?: destination
        // } else if (!depositEntries.isNullOrEmpty()) {
        //     depositEntries.first().destination
        // } else {
        //     destination
        // }
    }

    fun getTag(): String? {
        // return if (assetId == BITCOIN_CHAIN_ID) {
        //     depositEntries?.firstOrNull { depositEntry ->
        //         depositEntry.properties != null && depositEntry.destination.isNotBlank() && depositEntry.properties.any { property ->
        //             property.equals(
        //                 "SegWit",
        //                 false,
        //             )
        //         }
        //     }?.tag
        // } else if (!depositEntries.isNullOrEmpty()) {
        //     depositEntries.first().tag
        // } else {
        //     tag
        // }
        return ""
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TokenItem>() {
            override fun areItemsTheSame(oldItem: TokenItem, newItem: TokenItem) =
                oldItem.assetId == newItem.assetId

            override fun areContentsTheSame(oldItem: TokenItem, newItem: TokenItem) =
                oldItem == newItem
        }
    }
}

fun TokenItem.toPriceAndChange(): PriceAndChange {
    return PriceAndChange(assetId, priceBtc, priceUsd, changeUsd, changeBtc)
}

fun TokenItem.differentProcess(
    keyAction: () -> Unit,
    memoAction: () -> Unit,
    errorAction: () -> Unit,
) {
    when {
        getDestination().isNotEmpty() && !getTag().isNullOrEmpty() -> memoAction()
        getDestination().isNotEmpty() -> keyAction()
        else -> errorAction()
    }
}

fun TokenItem.needShowReserve(): Boolean {
    if (reserve.isNullOrBlank()) return false

    val reserveVal = reserve.toIntOrNull() ?: return false
    return reserveVal > 0
}
