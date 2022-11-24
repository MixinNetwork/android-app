package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.recyclerview.widget.DiffUtil
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
@Entity
data class SnapshotItem(
    @PrimaryKey
    @SerializedName("snapshot_id")
    @ColumnInfo(name = "snapshot_id")
    val snapshotId: String,
    @SerializedName("type")
    @ColumnInfo(name = "type")
    val type: String,
    @SerializedName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @SerializedName("amount")
    @ColumnInfo(name = "amount")
    val amount: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @SerializedName("opponent_id")
    @ColumnInfo(name = "opponent_id")
    val opponentId: String?,
    @SerializedName("opponent_ful_name")
    @ColumnInfo(name = "opponent_ful_name")
    val opponentFullName: String?,
    @SerializedName("transaction_hash")
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String?,
    @SerializedName("sender")
    @ColumnInfo(name = "sender")
    val sender: String?,
    @SerializedName("receiver")
    @ColumnInfo(name = "receiver")
    val receiver: String?,
    @SerializedName("memo")
    @ColumnInfo(name = "memo")
    val memo: String?,
    @SerializedName("asset_symbol")
    @ColumnInfo(name = "asset_symbol")
    val assetSymbol: String?,
    @SerializedName("confirmations")
    @ColumnInfo(name = "confirmations")
    val confirmations: Int?,
    @SerializedName("avatar_url")
    @ColumnInfo(name = "avatar_url")
    val avatarUrl: String?,
    @SerializedName("asset_confirmations")
    @ColumnInfo(name = "asset_confirmations")
    val assetConfirmations: Int,
    @SerializedName("trace_id")
    @ColumnInfo(name = "trace_id")
    val traceId: String?,
) : Parcelable {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<SnapshotItem>() {
            override fun areItemsTheSame(oldItem: SnapshotItem, newItem: SnapshotItem) =
                oldItem.snapshotId == newItem.snapshotId

            override fun areContentsTheSame(oldItem: SnapshotItem, newItem: SnapshotItem) =
                oldItem == newItem
        }

        fun fromSnapshot(snapshot: Snapshot, avatarUrl: String? = null, symbol: String? = null) = SnapshotItem(
            snapshotId = snapshot.snapshotId,
            type = snapshot.type,
            assetId = snapshot.assetId,
            amount = snapshot.amount,
            createdAt = snapshot.createdAt,
            opponentId = snapshot.opponentId,
            opponentFullName = null,
            transactionHash = snapshot.transactionHash,
            sender = snapshot.sender,
            receiver = snapshot.receiver,
            memo = snapshot.memo,
            assetSymbol = symbol,
            confirmations = snapshot.confirmations,
            avatarUrl = avatarUrl,
            assetConfirmations = 0,
            traceId = snapshot.traceId
        )
    }
}
