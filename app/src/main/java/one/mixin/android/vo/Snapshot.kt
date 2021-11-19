package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Entity(tableName = "snapshots")
@JsonClass(generateAdapter = true)
data class Snapshot(
    @PrimaryKey
    @SerializedName("snapshot_id")
    @ColumnInfo(name = "snapshot_id")
    @Json(name = "snapshot_id")
    val snapshotId: String,
    @SerializedName("type")
    @ColumnInfo(name = "type")
    @Json(name = "type")
    val type: String,
    @SerializedName("asset_id")
    @ColumnInfo(name = "asset_id")
    @Json(name = "asset_id")
    val assetId: String,
    @SerializedName("amount")
    @ColumnInfo(name = "amount")
    @Json(name = "amount")
    val amount: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    @Json(name = "created_at")
    val createdAt: String,
    @SerializedName("opponent_id")
    @ColumnInfo(name = "opponent_id")
    @Json(name = "opponent_id")
    val opponentId: String?,
    @SerializedName("trace_id")
    @ColumnInfo(name = "trace_id")
    @Json(name = "trace_id")
    val traceId: String?,
    @SerializedName("transaction_hash")
    @ColumnInfo(name = "transaction_hash")
    @Json(name = "transaction_hash")
    val transactionHash: String?,
    @SerializedName("sender")
    @ColumnInfo(name = "sender")
    @Json(name = "sender")
    val sender: String?,
    @SerializedName("receiver")
    @ColumnInfo(name = "receiver")
    @Json(name = "receiver")
    val receiver: String?,
    @SerializedName("memo")
    @ColumnInfo(name = "memo")
    @Json(name = "memo")
    val memo: String?,
    @SerializedName("confirmations")
    @ColumnInfo(name = "confirmations")
    @Json(name = "confirmations")
    val confirmations: Int?
)

@Suppress("EnumEntryName")
enum class SnapshotType { transfer, deposit, withdrawal, fee, rebate, raw, pending }
