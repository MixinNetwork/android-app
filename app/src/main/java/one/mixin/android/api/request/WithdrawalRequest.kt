package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WithdrawalRequest(
    @Json(name = "address_id")
    val addressId: String,
    val amount: String,
    val pin: String,
    @Json(name = "trace_id")
    val traceId: String,
    val memo: String?,
    val fee: String?,
)
