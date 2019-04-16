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
    val publicKey: String?,
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
    val accountName: String?,
    val accountTag: String?,
    val assetKey: String?
) : Parcelable {
    fun usd(): BigDecimal {
        return BigDecimal(balance) * BigDecimal(priceUsd)
    }

    fun btc(): BigDecimal {
        return BigDecimal(balance) * BigDecimal(priceBtc)
    }

    fun toAsset() = Asset(assetId, symbol, name, iconUrl, balance, publicKey, priceBtc, priceUsd,
        chainId, changeUsd, changeBtc, hidden, confirmations, accountName, accountTag, assetKey)

    fun isPublicKeyAsset(): Boolean {
        return !publicKey.isNullOrEmpty() && accountName.isNullOrEmpty() && accountTag.isNullOrEmpty()
    }

    fun isAccountTagAsset(): Boolean {
        return !accountName.isNullOrEmpty() && !accountTag.isNullOrEmpty() && publicKey.isNullOrEmpty()
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