package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class AssetItem(
    val assetId: String,
    val symbol: String,
    val name: String,
    val iconUrl: String,
    val balance: String,
    val publicKey: String,
    val priceBtc: String,
    val priceUsd: String,
    val chainId: String,
    val changeUsd: String,
    val changeBtc: String,
    var hidden: Boolean?,
    val chainIconUrl: String?
) : Parcelable {
    fun usd(): Float {
        return balance.toFloat() * priceUsd.toFloat()
    }

    fun btc(): Float {
        return balance.toFloat() * priceBtc.toFloat()
    }

    fun toAsset() = Asset(assetId, symbol, name, iconUrl, balance, publicKey, priceBtc, priceUsd, chainId, changeUsd, changeBtc, hidden)
}