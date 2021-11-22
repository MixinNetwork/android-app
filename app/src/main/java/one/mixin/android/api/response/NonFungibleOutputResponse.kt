package one.mixin.android.api.response

import android.os.Parcelable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
class NonFungibleOutputResponse(
    val type: String,
    @Json(name ="request_id")
    val requestId: String,
    val action: String,
    @Json(name ="user_id")
    val userId: String,
    @Json(name ="token_id")
    val tokenId: String,
    val amount: String,
    @Json(name ="transaction_hash")
    val transactionHash: String,
    @Json(name ="raw_transaction")
    val rawTransaction: String,
    @Json(name ="output_id")
    val outputId: String,
    @Json(name ="output_index")
    val outputIndex: Int,
    @Json(name ="senders_threshold")
    val sendersThreshold: Int,
    val senders: Array<String>,
    @Json(name ="receivers_threshold")
    val receiversThreshold: Int,
    val receivers: Array<String>,
    val memo: String,
    val state: String,
    @Json(name ="created_at")
    val createdAt: String,
    @Json(name ="updated_at")
    val updatedAt: String,
    @Json(name ="signed_by")
    val signedBy: String,
    @Json(name ="signed_tx")
    val signedTx: String,
) : Parcelable
