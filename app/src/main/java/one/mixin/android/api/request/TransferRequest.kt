package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class TransferRequest(
    @SerializedName("asset_id")
    val assetId: String? = null,
    @SerializedName("opponent_id")
    val opponentId: String? = null,
    @SerializedName("amount")
    val amount: String? = null,
    @SerializedName("pin")
    val pin: String? = null,
    @SerializedName("trace_id")
    val traceId: String? = null,
    @SerializedName("memo")
    val memo: String? = null,
    @SerializedName("address_id")
    val addressId: String? = null,
    val destination: String? = null,
    @SerializedName("raw_payment_url")
    val rawPaymentUrl: String? = null,
)

