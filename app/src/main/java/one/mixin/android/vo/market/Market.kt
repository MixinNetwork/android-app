package one.mixin.android.vo.market

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.converter.OptionalListConverter

@Entity(
    tableName = "markets",
)
@TypeConverters(OptionalListConverter::class)
@Parcelize
data class Market(
    @PrimaryKey
    @SerializedName("coin_id")
    @ColumnInfo(name = "coin_id")
    val coinId: String,
    @SerializedName("name")
    @ColumnInfo(name = "name")
    val name: String,
    @SerializedName("symbol")
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @SerializedName("icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @SerializedName("current_price")
    @ColumnInfo(name = "current_price")
    val currentPrice: String,
    @SerializedName("market_cap")
    @ColumnInfo(name = "market_cap")
    val marketCap: String,
    @SerializedName("market_cap_rank")
    @ColumnInfo(name = "market_cap_rank")
    val marketCapRank: String,
    @SerializedName("total_volume")
    @ColumnInfo(name = "total_volume")
    val totalVolume: String,
    @SerializedName("high_24h")
    @ColumnInfo(name = "high_24h")
    val high24h: String,
    @SerializedName("low_24h")
    @ColumnInfo(name = "low_24h")
    val low24h: String,
    @SerializedName("price_change_24h")
    @ColumnInfo(name = "price_change_24h")
    val priceChange24h: String,
    @SerializedName("price_change_percentage_1h")
    @ColumnInfo(name = "price_change_percentage_1h")
    val priceChangePercentage1H: String,
    @SerializedName("price_change_percentage_24h")
    @ColumnInfo(name = "price_change_percentage_24h")
    val priceChangePercentage24H: String,
    @SerializedName("price_change_percentage_7d")
    @ColumnInfo(name = "price_change_percentage_7d")
    val priceChangePercentage7D: String,
    @SerializedName("price_change_percentage_30d")
    @ColumnInfo(name = "price_change_percentage_30d")
    val priceChangePercentage30D: String,
    @SerializedName("market_cap_change_24h")
    @ColumnInfo(name = "market_cap_change_24h")
    val marketCapChange24h: String,
    @SerializedName("market_cap_change_percentage_24h")
    @ColumnInfo(name = "market_cap_change_percentage_24h")
    val marketCapChangePercentage24h: String,
    @SerializedName("circulating_supply")
    @ColumnInfo(name = "circulating_supply")
    val circulatingSupply: String,
    @SerializedName("total_supply")
    @ColumnInfo(name = "total_supply")
    val totalSupply: String,
    @SerializedName("max_supply")
    @ColumnInfo(name = "max_supply")
    val maxSupply: String,
    @SerializedName("ath")
    @ColumnInfo(name = "ath")
    val ath: String,
    @SerializedName("ath_change_percentage")
    @ColumnInfo(name = "ath_change_percentage")
    val athChangePercentage: String,
    @SerializedName("ath_date")
    @ColumnInfo(name = "ath_date")
    val athDate: String,
    @SerializedName("atl")
    @ColumnInfo(name = "atl")
    val atl: String,
    @SerializedName("atl_change_percentage")
    @ColumnInfo(name = "atl_change_percentage")
    val atlChangePercentage: String,
    @SerializedName("atl_date")
    @ColumnInfo(name = "atl_date")
    val atlDate: String,
    @SerializedName("asset_ids")
    @ColumnInfo(name = "asset_ids")
    val assetIds: List<String>?,
    @SerializedName("sparkline_in_7d")
    @ColumnInfo(name = "sparkline_in_7d")
    val sparklineIn7d: String,
    @SerializedName("sparkline_in_24h")
    @ColumnInfo(name = "sparkline_in_24h")
    val sparklineIn24h: String,
    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
) : Parcelable