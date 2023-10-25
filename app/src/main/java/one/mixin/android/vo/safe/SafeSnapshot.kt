package one.mixin.android.vo.safe

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "safe_snapshots")
@Serializable
data class SafeSnapshot(
    @PrimaryKey
    @SerializedName("snapshot_id")
    @SerialName("snapshot_id")
    @ColumnInfo(name = "snapshot_id")
    val snapshotId: String,

    @SerializedName("type")
    @SerialName("type")
    @ColumnInfo(name = "type")
    val type: String,

    @SerializedName("asset_id")
    @SerialName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,

    @SerializedName("amount")
    @SerialName("amount")
    @ColumnInfo(name = "amount")
    val amount: String,

    @SerializedName("created_at")
    @SerialName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,

    @SerializedName("opponent_id")
    @SerialName("opponent_id")
    @ColumnInfo(name = "opponent_id")
    val opponentId: String?,

    @SerializedName("trace_id")
    @SerialName("trace_id")
    @ColumnInfo(name = "trace_id")
    val traceId: String?,

    @SerializedName("transaction_hash")
    @SerialName("transaction_hash")
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String?,

    @SerializedName("sender")
    @SerialName("sender")
    @ColumnInfo(name = "sender")
    val sender: String?,

    @SerializedName("receiver")
    @SerialName("receiver")
    @ColumnInfo(name = "receiver")
    val receiver: String?,

    @SerializedName("memo")
    @SerialName("memo")
    @ColumnInfo(name = "memo")
    val memo: String?,

    @SerializedName("confirmations")
    @SerialName("confirmations")
    @ColumnInfo(name = "confirmations")
    val confirmations: Int?,

    @SerializedName("snapshot_hash")
    @SerialName("snapshot_hash")
    @ColumnInfo(name = "snapshot_hash")
    val snapshotHash: String?,

    @SerializedName("opening_balance")
    @SerialName("opening_balance")
    @ColumnInfo(name = "opening_balance")
    val openingBalance: String?,

    @SerializedName("closing_balance")
    @SerialName("closing_balance")
    @ColumnInfo(name = "closing_balance")
    val closingBalance: String?,
)

