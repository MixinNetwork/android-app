package one.mixin.android.api.request

import com.google.gson.annotations.SerializedName

data class RawTransactionsRequest(
    @SerializedName("asset_id")
    val assetId: String,
    val receivers: Array<String>,
    val threshold: Int,
    val amount: String,
    var pin: String,
    @SerializedName("trace_id")
    val tranceId: String?,
    val memo: String?
)
