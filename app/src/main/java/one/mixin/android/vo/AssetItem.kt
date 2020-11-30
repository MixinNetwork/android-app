package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@SuppressLint("ParcelCreator")
@Parcelize
data class AssetItem(
    val assetId: String,
    val symbol: String,
    val name: String,
    val iconUrl: String,
    val balance: String,
    val destination: String,
    val tag: String?,
    val priceBtc: String,
    val priceUsd: String,
    val chainId: String,
    val changeUsd: String,
    val changeBtc: String,
    var hidden: Boolean?,
    val confirmations: Int,
    val chainIconUrl: String?,
    val chainSymbol: String?,
    val chainName: String?,
    val chainPriceUsd: String?,
    val assetKey: String?,
    val reserve: String?
) : Parcelable {
    fun fiat(): BigDecimal {
        return BigDecimal(balance).multiply(priceFiat())
    }

    fun priceFiat(): BigDecimal = if (priceUsd == "0") {
        BigDecimal.ZERO
    } else BigDecimal(priceUsd).multiply(BigDecimal(Fiats.getRate()))

    fun chainPriceFiat(): BigDecimal = if (chainPriceUsd == null || chainPriceUsd == "0") {
        BigDecimal.ZERO
    } else BigDecimal(chainPriceUsd).multiply(BigDecimal(Fiats.getRate()))

    fun btc(): BigDecimal = if (priceBtc == "0") {
        BigDecimal.ZERO
    } else BigDecimal(balance).multiply(BigDecimal(priceBtc))

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AssetItem>() {
            override fun areItemsTheSame(oldItem: AssetItem, newItem: AssetItem) =
                oldItem.assetId == newItem.assetId

            override fun areContentsTheSame(oldItem: AssetItem, newItem: AssetItem) =
                oldItem == newItem
        }
    }
}

fun AssetItem.differentProcess(
    keyAction: () -> Unit,
    memoAction: () -> Unit,
    errorAction: () -> Unit
) {
    when {
        destination.isNotEmpty() && !tag.isNullOrEmpty() -> memoAction()
        destination.isNotEmpty() -> keyAction()
        else -> errorAction()
    }
}

fun AssetItem.needShowReserve(): Boolean {
    if (reserve.isNullOrBlank()) return false

    val reserveVal = reserve.toIntOrNull() ?: return false
    return reserveVal > 0
}
