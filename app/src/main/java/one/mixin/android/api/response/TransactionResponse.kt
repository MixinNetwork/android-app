package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
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
    val receivers: List<Receiver>?,
    val signers: List<String>?,
    @SerializedName("revoked_by")
    val revokedBy: String?,
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
    @SerializedName("inscription_hash")
    val inscriptionHash: String?,
    @SerializedName("safe")
    val safe: SafeAccount?
) {
    val getSnapshotId: String
        get() {
            if (snapshotId.isNullOrBlank()) {
                return nameUUIDFromBytes("$userId:$transactionHash".toByteArray()).toString()
            }
            return snapshotId
        }
}

data class Receiver(
    @SerializedName("members")
    val members: List<String>,
    @SerializedName("members_hash")
    val membersHash: String,
    val threshold: Int,
    val destination: String?,
    val tag: String?,
    @SerializedName("withdrawal_hash")
    val withdrawalHash: String?,
)

@Parcelize
data class SafeTransactionRecipient(
    val address: String,
    val amount: String,
    var label: String?
) : Parcelable

@Parcelize
data class SafeTransaction(
    @SerializedName("asset_id")
    val assetId: String,
    val recipients: List<SafeTransactionRecipient>,
) : Parcelable

@Parcelize
data class SafeOperation(
    val note: String?,
    val transaction: SafeTransaction
) : Parcelable

@Parcelize
data class SafeAccount(
    val id: String,
    val name: String,
    val address: String,
    val operation: SafeOperation
) : Parcelable

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
