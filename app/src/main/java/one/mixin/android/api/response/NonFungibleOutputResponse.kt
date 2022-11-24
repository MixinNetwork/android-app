package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class NonFungibleOutputResponse(
    val type: String,
    @SerializedName("request_id")
    val requestId: String,
    val action: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("token_id")
    val tokenId: String,
    val amount: String,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    @SerializedName("raw_transaction")
    val rawTransaction: String,
    @SerializedName("output_id")
    val outputId: String,
    @SerializedName("output_index")
    val outputIndex: Int,
    @SerializedName("senders_threshold")
    val sendersThreshold: Int,
    val senders: Array<String>,
    @SerializedName("receivers_threshold")
    val receiversThreshold: Int,
    val receivers: Array<String>,
    val memo: String,
    val state: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("signed_by")
    val signedBy: String,
    @SerializedName("signed_tx")
    val signedTx: String,
) : Parcelable
