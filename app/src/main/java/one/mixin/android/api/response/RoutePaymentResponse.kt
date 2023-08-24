package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class RoutePaymentResponse(
    @SerializedName("payment_id")
    val paymentId: String,
    @SerializedName("amount")
    val amount: Int,
    @SerializedName("asset_amount")
    val assetAmount: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("session_id")
    val sessionId: String,
    @SerializedName("status")
    val status: String,
)

enum class RoutePaymentStatus { Authorized, Captured }
