package one.mixin.android.vo.safe

import com.google.gson.annotations.SerializedName

data class PendingDeposit(
    @SerializedName("deposit_id")
    val depositId: String,
    @SerializedName("asset_id")
    val assetId: String,
    val destination: String,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    val amount: String,
    val confirmations: Int,
    @SerializedName("created_at")
    val createdAt: String,
)

fun PendingDeposit.toSnapshot(): SafeSnapshot =
    SafeSnapshot(
        this.depositId, SafeSnapshotType.pending.name, this.assetId, this.amount, "",
        "", "", "", this.createdAt, "", this.confirmations, "", "", SafeDeposit(this.transactionHash), null,
    )
