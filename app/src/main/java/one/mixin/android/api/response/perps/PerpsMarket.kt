package one.mixin.android.api.response.perps

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.ColumnTypeConverters
import com.google.gson.annotations.SerializedName
import one.mixin.android.db.converter.DescriptionsConverter
import one.mixin.android.db.converter.ListConverter

@Entity(tableName = "markets")
@ColumnTypeConverters(ListConverter::class, DescriptionsConverter::class)
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

    @SerializedName("price_scale")
    @ColumnInfo(name = "price_scale", defaultValue = "2")
    val priceScale: Int = 2,

    @SerializedName("leverage")
    @ColumnInfo(name = "leverage")
    val leverage: Int,

    @SerializedName("icon_url")
    @ColumnInfo(name = "icon_url")
    val iconUrl: String,

    @SerializedName("category")
    @ColumnInfo(name = "category")
    val category: String = "",

    @SerializedName("tags")
    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList(),

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

    @SerializedName("descriptions")
    @ColumnInfo(name = "descriptions")
    val descriptions: Map<String, String>? = null,
)

fun PerpsMarket.withDefaults(): PerpsMarket =
    copy(
        category = (category as String?) ?: "",
        tags = (tags as? List<*>)?.mapNotNull { it as? String }.orEmpty(),
        priceScale = priceScale.coerceAtLeast(0),
    )
