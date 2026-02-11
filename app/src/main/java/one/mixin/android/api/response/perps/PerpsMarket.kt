package one.mixin.android.api.response.perps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "perps_markets")
data class PerpsMarket(
    @PrimaryKey
    @SerializedName("market_id")
    @ColumnInfo(name = "market_id")
    val marketId: String,
    @SerializedName("market")
    @ColumnInfo(name = "market")
    val market: String,
    @SerializedName("symbol")
    @ColumnInfo(name = "symbol")
    val symbol: String,
    @SerializedName("mark_price")
    @ColumnInfo(name = "mark_price")
    val markPrice: String,
    @SerializedName("funding_rate")
    @ColumnInfo(name = "funding_rate")
    val fundingRate: String,
    @SerializedName("maker_fee")
    @ColumnInfo(name = "maker_fee")
    val makerFee: String,
    @SerializedName("taker_fee")
    @ColumnInfo(name = "taker_fee")
    val takerFee: String,
    @SerializedName("min_order_size")
    @ColumnInfo(name = "min_order_size")
    val minOrderSize: String,
    @SerializedName("max_order_size")
    @ColumnInfo(name = "max_order_size")
    val maxOrderSize: String,
    @SerializedName("min_order_value")
    @ColumnInfo(name = "min_order_value")
    val minOrderValue: String,
    @SerializedName("quantity_increment")
    @ColumnInfo(name = "quantity_increment")
    val quantityIncrement: String,
    @SerializedName("price_increment")
    @ColumnInfo(name = "price_increment")
    val priceIncrement: String,
    @SerializedName("last")
    @ColumnInfo(name = "last")
    val last: String,
    @SerializedName("volume")
    @ColumnInfo(name = "volume")
    val volume: String,
    @SerializedName("leverage")
    @ColumnInfo(name = "leverage")
    val leverage: Int,
    @SerializedName("icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,
    @SerializedName("change")
    @ColumnInfo(name = "change")
    val change: String,
    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String
)
