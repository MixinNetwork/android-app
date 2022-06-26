package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "icon_url")
    @SerializedName("icon_url")
    val iconUrl: String,
    @ColumnInfo(name = "balance")
    val balance: String,
    @SerializedName("destination")
    @ColumnInfo(name = "destination")
    var destination: String,
    @SerializedName("tag")
    @ColumnInfo(name = "tag")
    val tag: String?,
    @SerializedName("price_btc")
    @ColumnInfo(name = "price_btc")
    val priceBtc: String,
    @SerializedName("price_usd")
    @ColumnInfo(name = "price_usd")
    val priceUsd: String,
    @SerializedName("chain_id")
    @ColumnInfo(name = "chain_id")
    val chainId: String,
    @SerializedName("change_usd")
    @ColumnInfo(name = "change_usd")
    val changeUsd: String,
    @SerializedName("change_btc")
    @ColumnInfo(name = "change_btc")
    val changeBtc: String,
    @ColumnInfo(name = "confirmations")
    val confirmations: Int,
    @SerializedName("asset_key")
    @ColumnInfo(name = "asset_key")
    val assetKey: String?,
    @SerializedName("reserve")
    @ColumnInfo(name = "reserve")
    val reserve: String?,
    @SerializedName("deposit_entries")
    @Ignore
    val depositEntries: List<DepositEntry>? = null
) : Parcelable {

    constructor(
        assetId: String,
        symbol: String,
        name: String,
        iconUrl: String,
        balance: String,
        destination: String,
        tag: String?,
        priceBtc: String,
        priceUsd: String,
        chainId: String,
        changeUsd: String,
        changeBtc: String,
        confirmations: Int,
        assetKey: String?,
        reserve: String?
    ) :
        this(
            assetId,
            symbol,
            name,
            iconUrl,
            balance,
            destination,
            tag,
            priceBtc,
            priceUsd,
            chainId,
            changeUsd,
            changeBtc,
            confirmations,
            assetKey,
            reserve,
            null
        )
}

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

fun Asset.toAssetItem(chainIconUrl: String? = null): AssetItem = AssetItem(
    assetId, symbol, name, iconUrl, balance, destination, tag, priceBtc, priceUsd, chainId, changeUsd, changeBtc, false,
    confirmations, chainIconUrl, null, null, null, assetKey, reserve
)

fun Asset.toTopAssetItem(chainIconUrl: String?) = TopAssetItem(assetId, symbol, name, iconUrl, chainId, chainIconUrl, priceUsd, changeUsd)

fun Asset.replaceDestination() {
    if (depositEntries != null) {
        depositEntries.firstOrNull { depositEntry ->
            depositEntry.properties != null && depositEntry.destination.isNotBlank() && depositEntry.properties.any { property ->
                property.equals(
                    "SegWit",
                    false
                )
            }
        }?.let { depositEntry ->
            destination = depositEntry.destination
        }
    }
}
