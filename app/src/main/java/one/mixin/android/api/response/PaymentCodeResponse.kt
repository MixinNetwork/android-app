package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName

data class PaymentCodeResponse(
    @SerializedName("code_id")
    val codeId: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("receivers")
    val receivers: Array<String>,
    @SerializedName("threshold")
    val threshold: Int,
    @SerializedName("status")
    val status: String,
    @SerializedName("memo")
    val memo: String,
    @SerializedName("trace_id")
    val traceId: String,
    @SerializedName("created_at")
    val createdAt: String,
)
