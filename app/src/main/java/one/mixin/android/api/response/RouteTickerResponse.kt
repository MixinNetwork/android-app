package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class RouteTickerResponse(
    @SerializedName("currency")
    val currency: String,
    @SerializedName("total_amount")
    val totalAmount: String,
    @SerializedName("purchase")
    val purchase: String,
    @SerializedName("fee")
    val fee: String,
    @SerializedName("price")
    val price: String,
    @SerializedName("asset_price")
    val assetPrice: String,
    @SerializedName("asset_amount")
    val assetAmount: String,
    @SerializedName("fee_percent")
    val feePercent: String,
    @SerializedName("minimum")
    val minimum: String,
    @SerializedName("maximum")
    val maximum: String,
)
