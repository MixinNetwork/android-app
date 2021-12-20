package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "snapshots")
@JsonClass(generateAdapter = true)
data class Snapshot(
    @PrimaryKey
    @Json(name = "snapshot_id")
    @ColumnInfo(name = "snapshot_id")
    val snapshotId: String,
    @Json(name = "type")
    @ColumnInfo(name = "type")
    val type: String,
    @Json(name = "asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @Json(name = "amount")
    @ColumnInfo(name = "amount")
    val amount: String,
    @Json(name = "created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @Json(name = "opponent_id")
    @ColumnInfo(name = "opponent_id")
    val opponentId: String?,
    @Json(name = "trace_id")
    @ColumnInfo(name = "trace_id")
    val traceId: String?,
    @Json(name = "transaction_hash")
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String?,
    @Json(name = "sender")
    @ColumnInfo(name = "sender")
    val sender: String?,
    @Json(name = "receiver")
    @ColumnInfo(name = "receiver")
    val receiver: String?,
    @Json(name = "memo")
    @ColumnInfo(name = "memo")
    val memo: String?,
    @Json(name = "confirmations")
    @ColumnInfo(name = "confirmations")
    val confirmations: Int?
)

@Suppress("EnumEntryName")
enum class SnapshotType { transfer, deposit, withdrawal, fee, rebate, raw, pending }
