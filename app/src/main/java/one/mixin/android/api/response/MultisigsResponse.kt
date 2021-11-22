package one.mixin.android.api.response

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import one.mixin.android.vo.CodeResponse

@Parcelize
@JsonClass(generateAdapter = true)
class MultisigsResponse(
    val type: String,
    @Json(name = "code_id")
    val codeId: String,
    @Json(name = "request_id")
    val requestId: String,
    val action: String,
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "asset_id")
    val assetId: String,
    val amount: String,
    val senders: Array<String>,
    val receivers: Array<String>,
    val threshold: Int,
    val state: String,
    @Json(name = "transaction_hash")
    val transactionHash: String,
    @Json(name = "raw_transaction")
    val rawTransaction: String,
    @Json(name = "created_at")
    val createdAt: String,
    val memo: String?
) : Parcelable, CodeResponse
