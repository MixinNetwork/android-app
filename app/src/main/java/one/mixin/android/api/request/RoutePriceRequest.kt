package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class RoutePriceRequest(
    @SerializedName("asset_amount")
    val assetAmount: String
)
