package one.mixin.android.vo.safe

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "raw_transactions",
    indices = [
        Index(value = arrayOf("state", "type")),
    ],
)
data class RawTransaction(
    @PrimaryKey
    @ColumnInfo(name = "request_id")
    val requestId: String,
    @ColumnInfo(name = "raw_transaction")
    val rawTransaction: String,
    @ColumnInfo(name = "receiver_id")
    val receiverId: String,
    @ColumnInfo(name = "type")
    val type: RawTransactionType,
    @ColumnInfo(name = "state")
    val state: OutputState,
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @ColumnInfo(name = "inscription_hash")
    val inscriptionHash: String?,
)

fun formatDestination(
    destination: String,
    tag: String?,
): String {
    return if (tag.isNullOrEmpty()) {
        destination
    } else {
        "$destination:$tag"
    }
}
