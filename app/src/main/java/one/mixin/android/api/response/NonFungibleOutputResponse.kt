package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class NonFungibleOutputResponse(
    val type: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("output_id")
    val outputId: String,
    @SerializedName("token_id")
    val tokenId: String,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    @SerializedName("output_index")
    val outputIndex: Int,
    val amount: String,
    @SerializedName("senders_threshold")
    val sendersThreshold: String,
    val senders: List<String>,
    @SerializedName("receivers_threshold")
    val receiversThreshold: String,
    val receivers: List<String>,
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
