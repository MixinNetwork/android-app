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
@Entity(tableName = "top_assets")
@JsonClass(generateAdapter = true)
data class TopAsset(
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
    val confirmations: Int
) : Parcelable

fun TopAsset.toPriceAndChange(): PriceAndChange {
    return PriceAndChange(assetId, priceBtc, priceUsd, changeUsd, changeBtc)
}
