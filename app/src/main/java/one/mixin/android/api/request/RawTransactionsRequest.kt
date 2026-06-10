package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class RawTransactionsRequest(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("opponent_multisig")
    val opponentMultisig: OpponentMultisig,
    val amount: String,
    var pin: String,
    @SerializedName("trace_id")
    val traceId: String?,
    val memo: String?,
)

data class OpponentMultisig(
    val receivers: Array<String>,
    val threshold: Int,
)
