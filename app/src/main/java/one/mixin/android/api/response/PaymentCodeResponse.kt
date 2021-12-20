package one.mixin.android.api.response

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import one.mixin.android.vo.CodeResponse

@JsonClass(generateAdapter = true)
data class PaymentCodeResponse(
    @Json(name = "code_id")
    val codeId: String,
    @Json(name = "asset_id")
    val assetId: String,
    val amount: String,
    val receivers: Array<String>,
    val threshold: Int,
    val status: String,
    val memo: String,
    @Json(name = "trace_id")
    val traceId: String,
    @Json(name = "created_at")
    val createdAt: String
) : CodeResponse
