package one.mixin.android.vo

import com.google.gson.annotations.SerializedName

data class PendingDeposit(
    val type: String,
    @SerializedName("transaction_id")
    val transactionId: String,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    val sender: String,
    val amount: String,
    val confirmations: Int,
    val threshold: Int,
    @SerializedName("created_at")
    val createdAt: String
)

fun PendingDeposit.toSnapshot(assetId: String): Snapshot =
    Snapshot(
        this.transactionId, SnapshotType.pending.name, assetId, this.amount, this.createdAt,
        null, null, this.transactionHash, this.sender, null, null, this.confirmations
    )
