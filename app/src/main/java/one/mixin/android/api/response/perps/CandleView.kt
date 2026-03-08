package one.mixin.android.api.response.perps

import com.google.gson.annotations.SerializedName

data class CandleView(
    @SerializedName("market")
    val market: String,
    @SerializedName("product")
    val product: String,
    @SerializedName("time_frame")
    val timeFrame: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("items")
    val items: List<CandleItem>
)

data class CandleItem(
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("open")
    val open: String,
    @SerializedName("high")
    val high: String,
    @SerializedName("low")
    val low: String,
    @SerializedName("close")
    val close: String,
    @SerializedName("volume")
    val volume: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("count")
    val count: Long
)
