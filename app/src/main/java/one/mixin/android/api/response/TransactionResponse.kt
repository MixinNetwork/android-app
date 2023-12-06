package one.mixin.android.api.response

import com.google.gson.annotations.SerializedName
import java.util.UUID.nameUUIDFromBytes

data class TransactionResponse(
    val type: String,
    @SerializedName("request_id")
    val requestId: String,
    @SerializedName("user_id")
    val userId: String,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    @SerializedName("snapshot_id")
    val snapshotId: String?,
    val asset: String,
    @SerializedName("asset_id")
    val assetId: String?,
    @SerializedName("senders_hash")
    val sendersHash: String,
    @SerializedName("senders_threshold")
    val sendersThreshold: Int,
    val senders: List<String>,
    val signers: List<String>,
    val extra: String,
    val state: String,
    @SerializedName("raw_transaction")
    val rawTransaction: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("snapshot_hash")
    val snapshotHash: String,
    @SerializedName("snapshot_at")
    val snapshotAt: String,
    @SerializedName("views")
    val views: List<String>?,
) {
    val getSnapshotId: String
        get() {
            if (snapshotId.isNullOrBlank()) {
                return nameUUIDFromBytes("$userId:$transactionHash".toByteArray()).toString()
            }
            return snapshotId
        }
}

fun getTransactionResult(
    transactionList: List<TransactionResponse>?,
    firstRequestId: String,
    secondRequestId: String?,
): Pair<TransactionResponse, TransactionResponse?> {
    transactionList ?: throw NullPointerException("Empty response")
    var firstTransaction: TransactionResponse? = null
    var secondTransaction: TransactionResponse? = null
    transactionList.forEach { transaction ->
        if (transaction.requestId == firstRequestId) {
            firstTransaction = transaction
        } else if (secondRequestId != null && transaction.requestId == secondRequestId) {
            secondTransaction = transaction
        }
    }
    if (firstTransaction == null) {
        throw NullPointerException("The first transaction was not found")
    } else {
        return Pair(firstTransaction!!, secondTransaction)
    }
}
