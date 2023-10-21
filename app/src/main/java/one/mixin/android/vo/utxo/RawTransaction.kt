package one.mixin.android.vo.utxo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "raw_transaction")
data class RawTransaction(
    @PrimaryKey
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String,
    @ColumnInfo(name = "raw_transaction")
    val rawTransaction: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)