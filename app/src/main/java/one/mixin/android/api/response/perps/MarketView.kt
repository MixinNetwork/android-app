package one.mixin.android.api.response.perps

import com.google.gson.annotations.SerializedName

data class MarketView(
    @SerializedName("market_id")
    val marketId: String,
    @SerializedName("market")
    val market: String,
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("mark_price")
    val markPrice: String,
    @SerializedName("funding_rate")
    val fundingRate: String,
    @SerializedName("maker_fee")
    val makerFee: String,
    @SerializedName("taker_fee")
    val takerFee: String,
    @SerializedName("min_order_size")
    val minOrderSize: String,
    @SerializedName("max_order_size")
    val maxOrderSize: String,
    @SerializedName("min_order_value")
    val minOrderValue: String,
    @SerializedName("quantity_increment")
    val quantityIncrement: String,
    @SerializedName("price_increment")
    val priceIncrement: String,
    @SerializedName("last")
    val last: String,
    @SerializedName("volume")
    val volume: String,
    @SerializedName("leverage")
    val leverage: Int,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("change")
    val change: String,
    @SerializedName("updated_at")
    val updatedAt: String
)
