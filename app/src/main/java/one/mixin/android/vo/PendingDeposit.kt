package one.mixin.android.vo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PendingDeposit(
    val type: String,
    @Json(name = "transaction_id")
    val transactionId: String,
    @Json(name = "transaction_hash")
    val transactionHash: String,
    val sender: String,
    val amount: String,
    val confirmations: Int,
    val threshold: Int,
    @Json(name = "created_at")
    val createdAt: String
)

fun PendingDeposit.toSnapshot(assetId: String): Snapshot =
    Snapshot(
        this.transactionId, SnapshotType.pending.name, assetId, this.amount, this.createdAt,
        null, null, this.transactionHash, this.sender, null, null, this.confirmations
    )
