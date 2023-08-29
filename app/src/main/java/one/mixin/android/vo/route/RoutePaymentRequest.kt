package one.mixin.android.vo.route

import com.google.gson.annotations.SerializedName

class RoutePaymentRequest(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("instrument_id")
    val instrumentId: String,
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("asset_amount")
    val assetAmount: String,
    @SerializedName("currency")
    val currency: String,
)
