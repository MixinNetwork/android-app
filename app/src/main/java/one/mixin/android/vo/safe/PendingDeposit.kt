package one.mixin.android.vo.safe

import com.google.gson.annotations.SerializedName
import one.mixin.android.vo.SnapshotType

data class PendingDeposit(
    @SerializedName("deposit_id")
    val depositId: String,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    val amount: String,
    val confirmations: Int,
    val threshold: Int,
    @SerializedName("created_at")
    val createdAt: String,
)

fun PendingDeposit.toSnapshot(assetId: String): SafeSnapshot =
    SafeSnapshot(
        this.depositId, SnapshotType.pending.name, assetId, this.amount, this.createdAt,
        null, null, this.transactionHash, "", null, null, this.confirmations, null, "", "",
    )
