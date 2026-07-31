package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class RawTransactionsRequest(
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("opponent_multisig")
    val opponentMultisig: OpponentMultisig,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("pin")
    var pin: String,
    @SerializedName("trace_id")
    val traceId: String?,
    @SerializedName("memo")
    val memo: String?,
)

data class OpponentMultisig(
    @SerializedName("receivers")
    val receivers: Array<String>,
    @SerializedName("threshold")
    val threshold: Int,
)
