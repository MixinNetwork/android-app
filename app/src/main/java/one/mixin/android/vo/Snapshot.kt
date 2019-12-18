package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "snapshots")
data class Snapshot(
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
    @SerializedName("trace_id")
    @ColumnInfo(name = "trace_id")
    val traceId: String?,
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
    @SerializedName("confirmations")
    @ColumnInfo(name = "confirmations")
    val confirmations: Int?
)

@Suppress("EnumEntryName")
enum class SnapshotType { transfer, deposit, withdrawal, fee, rebate, raw, pending }
