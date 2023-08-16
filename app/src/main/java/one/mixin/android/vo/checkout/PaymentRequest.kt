package one.mixin.android.vo.checkout

import com.google.gson.annotations.SerializedName

class PaymentRequest(
    @SerializedName("token")
    val token: String,
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
    @SerializedName("currency")
    val currency: String,
)
