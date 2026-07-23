package one.mixin.android.api.response.perps

import com.google.gson.annotations.SerializedName

data class MarketLiquidationPriceView(
    @SerializedName("liquidation_price")
    val liquidationPrice: String? = null,
)
