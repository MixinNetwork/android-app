package one.mixin.android.api.response.perps

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "markets")
data class PerpsMarket(
    @PrimaryKey @SerializedName("market_id")
    @ColumnInfo(name = "market_id")
    val marketId: String,

    @SerializedName("display_symbol")
    @ColumnInfo(name = "display_symbol")
    val displaySymbol: String,

    @SerializedName("token_symbol")
    @ColumnInfo(name = "token_symbol")
    val tokenSymbol: String,

    @SerializedName("quote_symbol")
    @ColumnInfo(name = "quote_symbol")
    val quoteSymbol: String,

    @SerializedName("mark_price")
    @ColumnInfo(name = "mark_price")
    val markPrice: String,

    @SerializedName("leverage")
    @ColumnInfo(name = "leverage")
    val leverage: Int,

    @SerializedName("icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,

    @SerializedName("funding_rate")
    @ColumnInfo(name = "funding_rate")
    val fundingRate: String,

    @SerializedName("min_amount")
    @ColumnInfo(name = "min_amount")
    val minAmount: String,

    @SerializedName("max_amount")
    @ColumnInfo(name = "max_amount")
    val maxAmount: String,

    @SerializedName("last")
    @ColumnInfo(name = "last")
    val last: String,

    @SerializedName("volume")
    @ColumnInfo(name = "volume")
    val volume: String,

    @SerializedName("amount")
    @ColumnInfo(name = "amount")
    val amount: String,

    @SerializedName("high")
    @ColumnInfo(name = "high")
    val high: String,

    @SerializedName("low")
    @ColumnInfo(name = "low")
    val low: String,

    @SerializedName("open")
    @ColumnInfo(name = "open")
    val open: String,

    @SerializedName("change")
    @ColumnInfo(name = "change")
    val change: String,

    @SerializedName("bid_price")
    @ColumnInfo(name = "bid_price")
    val bidPrice: String,

    @SerializedName("ask_price")
    @ColumnInfo(name = "ask_price")
    val askPrice: String,

    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)
