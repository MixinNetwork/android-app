package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class WithdrawalRequest(
    @SerializedName("address_id")
    val addressId: String,
    val amount: String,
    val pin: String,
    @SerializedName("trace_id")
    val traceId: String,
    val memo: String?,
    val fee: String?,
)
