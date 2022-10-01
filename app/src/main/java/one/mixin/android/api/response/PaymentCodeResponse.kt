package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class PaymentCodeResponse(
    @SerializedName("code_id")
    val codeId: String,
    @SerializedName("asset_id")
    val assetId: String,
    val amount: String,
    val receivers: Array<String>,
    val threshold: Int,
    val status: String,
    val memo: String,
    @SerializedName("trace_id")
    val traceId: String,
    @SerializedName("created_at")
    val createdAt: String,
)
