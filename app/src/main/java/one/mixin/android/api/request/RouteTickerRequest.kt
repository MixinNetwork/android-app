package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class RouteTickerRequest(
    @SerializedName("amount")
    val amount: Int,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("asset_id")
    val assetId: String,
)
