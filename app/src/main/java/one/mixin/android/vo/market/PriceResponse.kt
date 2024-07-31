package one.mixin.android.vo.market

import com.google.gson.annotations.SerializedName

class PriceResponse(
    @SerializedName("price")
    val price: String,
    @SerializedName("price_change_24h")
    val priceChange24h: String,
    @SerializedName("vol_24h")
    val vol24h: String,
)