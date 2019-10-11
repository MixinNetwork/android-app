package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import kotlinx.android.parcel.Parcelize
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
    val tag: String,
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
    val assetKey: String?
) : Parcelable {
    fun fiat(): BigDecimal {
        return BigDecimal(balance) * priceFiat()
    }

    fun priceFiat() = BigDecimal(priceUsd) * BigDecimal(Fiats.getRate())

    fun btc(): BigDecimal {
        return BigDecimal(balance) * BigDecimal(priceBtc)
    }

    fun toAsset() = Asset(
        assetId, symbol, name, iconUrl, balance, destination, tag, priceBtc, priceUsd,
        chainId, changeUsd, changeBtc, hidden, confirmations, assetKey
    )

    fun isPublicKeyAsset(): Boolean {
        return tag.isEmpty()
    }

    fun isAccountTagAsset(): Boolean {
        return tag.isNotEmpty()
    }

    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<AssetItem>() {
            override fun areItemsTheSame(oldItem: AssetItem, newItem: AssetItem) =
                oldItem.assetId == newItem.assetId

            override fun areContentsTheSame(oldItem: AssetItem, newItem: AssetItem) =
                oldItem == newItem
        }
    }
}

fun AssetItem.differentProcess(keyAction: () -> Unit, memoAction: () -> Unit, errorAction: () -> Unit) {
    when {
        isPublicKeyAsset() -> keyAction()
        isAccountTagAsset() -> memoAction()
        else -> errorAction()
    }
}
