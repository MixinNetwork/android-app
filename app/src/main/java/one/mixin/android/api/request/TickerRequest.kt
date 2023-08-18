package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class TickerRequest (
    @SerializedName("amount")
    val amount:Int,
    @SerializedName("currency")
    val currency:String,
    @SerializedName("asset_id")
    val assetId:String,
)