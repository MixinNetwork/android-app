package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "assets")
@JsonClass(generateAdapter = true)
data class Asset(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @Json(name = "asset_id")
    val assetId: String,
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon_url")
    @Json(name = "icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "balance")
    val balance: String,
    @Json(name = "destination")
    @ColumnInfo(name = "destination")
    val destination: String,
    @Json(name = "tag")
    @ColumnInfo(name = "tag")
    val tag: String?,
    @Json(name = "price_btc")
    @ColumnInfo(name = "price_btc")
    val priceBtc: String,
    @Json(name = "price_usd")
    @ColumnInfo(name = "price_usd")
    val priceUsd: String,
    @Json(name = "chain_id")
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @Json(name = "change_usd")
    @ColumnInfo(name = "change_usd")
    val changeUsd: String,
    @Json(name = "change_btc")
    @ColumnInfo(name = "change_btc")
    val changeBtc: String,
    @ColumnInfo(name = "confirmations")
    val confirmations: Int,
    @Json(name = "asset_key")
    @ColumnInfo(name = "asset_key")
    val assetKey: String?,
    @Json(name ="reserve")
    @ColumnInfo(name = "reserve")
    val reserve: String?
) : Parcelable

data class PriceAndChange(
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @ColumnInfo(name = "price_btc")
    val priceBtc: String,
    @ColumnInfo(name = "price_usd")
    val priceUsd: String,
    @ColumnInfo(name = "change_usd")
    val changeUsd: String,
    @ColumnInfo(name = "change_btc")
    val changeBtc: String,
)

fun Asset.toPriceAndChange(): PriceAndChange {
    return PriceAndChange(assetId, priceBtc, priceUsd, changeUsd, changeBtc)
}

fun Asset.toAssetItem(): AssetItem = AssetItem(
    assetId, symbol, name, iconUrl, balance, destination, tag, priceBtc, priceUsd, chainId, changeUsd, changeBtc, false,
    confirmations, null, null, null, null, assetKey, reserve
)

fun Asset.toTopAssetItem(chainIconUrl: String?) = TopAssetItem(assetId, symbol, name, iconUrl, chainId, chainIconUrl, priceUsd, changeUsd)
