package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

class TransferRequest(
    @SerializedName("asset_id")
    val assertId: String,
    @SerializedName("counter_user_id")
    val counterUserId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("pin")
    val pin: String?,
    @SerializedName("trace_id")
    val traceId: String? = null,
    @SerializedName("memo")
    val memo: String? = null
)
