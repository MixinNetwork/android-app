package one.mixin.android.vo.safe

import com.google.gson.annotations.SerializedName

data class PendingDeposit(
    @SerializedName("deposit_id")
    val depositId: String,
    @SerializedName("asset_id")
    val assetId: String,
    @SerializedName("destination")
    val destination: String,
    @SerializedName("tag")
    val tag: String?,
    @SerializedName("transaction_hash")
    val transactionHash: String,
    @SerializedName("sender")
    val sender: String?,
    @SerializedName("amount")
    val amount: String,
    @SerializedName("confirmations")
    val confirmations: Long,
    @SerializedName("created_at")
    val createdAt: String,
)

fun PendingDeposit.toSnapshot(): SafeSnapshot =
    SafeSnapshot(
        this.depositId,
        SafeSnapshotType.pending.name,
        this.assetId,
        this.amount,
        "",
        "",
        "",
        "",
        this.createdAt,
        "",
        this.confirmations,
        "",
        "",
        SafeDeposit(this.transactionHash, this.sender ?: ""),
        null,
        null,
    )

data class DestinationTag(
    val destination: String,
    val tag: String?,
)
