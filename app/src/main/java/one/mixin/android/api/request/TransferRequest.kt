package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class TransferRequest(
    @Json(name = "asset_id")
    val assertId: String,
    @Json(name = "opponent_id")
    val opponentId: String?,
    @Json(name = "amount")
    val amount: String,
    @Json(name = "pin")
    val pin: String?,
    @Json(name = "trace_id")
    val traceId: String? = null,
    @Json(name = "memo")
    val memo: String? = null,
    @Json(name = "address_id")
    val addressId: String? = null
)
