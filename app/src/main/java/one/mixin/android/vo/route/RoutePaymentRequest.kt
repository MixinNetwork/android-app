package one.mixin.android.vo.route

import com.google.gson.annotations.SerializedName

class RoutePaymentRequest(
    @SerializedName("amount")
    val amount: Long,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("asset_amount")
    val assetAmount: String,
    @SerializedName("token")
    val token: String?,
    @SerializedName("session_id")
    val sessionId: String?,
    @SerializedName("instrument_id")
    val instrumentId: String?,
    @SerializedName("phone")
    val phone: String?,
    @SerializedName("device_session_id")
    val deviceSessionId: String?,
)
