package one.mixin.android.api.request

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RawTransactionsRequest(
    @Json(name = "asset_id")
    val assetId: String,
    @Json(name ="opponent_multisig")
    val opponentMultisig: OpponentMultisig,
    val amount: String,
    var pin: String,
    @Json(name ="trace_id")
    val traceId: String?,
    val memo: String?
)

@JsonClass(generateAdapter = true)
data class OpponentMultisig(
    val receivers: Array<String>,
    val threshold: Int
)
