package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class MultisigsResponse(
    @SerializedName("type")
    val type: String,
    @SerializedName("code_id")
    val codeId: String,
    @SerializedName("request_id")
    val requestId: String,
    @SerializedName("action")
    val action: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("senders")
    val senders: Array<String>,
    @SerializedName("receivers")
    val receivers: Array<String>,
    @SerializedName("threshold")
    val threshold: Int,
    @SerializedName("state")
    val state: String,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    @SerializedName("raw_transaction")
    val rawTransaction: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("memo")
    val memo: String?,
) : Parcelable
