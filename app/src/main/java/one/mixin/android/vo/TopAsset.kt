package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity(tableName = "top_assets")
data class TopAsset(
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
    val destination: String,
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
) : Parcelable

fun TopAsset.toPriceAndChange(): PriceAndChange {
    return PriceAndChange(assetId, priceBtc, priceUsd, changeUsd, changeBtc)
}
