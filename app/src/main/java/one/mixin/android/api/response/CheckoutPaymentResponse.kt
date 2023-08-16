package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

class CheckoutPaymentResponse(
    @SerializedName("pay_id")
    val payId: String,
    @SerializedName("amount")
    val amount: Int,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("session_id")
    val sessionId: String,
)
