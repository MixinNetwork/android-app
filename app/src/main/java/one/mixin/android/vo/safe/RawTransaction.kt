package one.mixin.android.vo.safe

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "raw_transactions")
data class RawTransaction(
    @PrimaryKey
    @ColumnInfo(name = "request_id")
    val requestId: String,
    @ColumnInfo(name = "raw_transaction")
    val rawTransaction: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)