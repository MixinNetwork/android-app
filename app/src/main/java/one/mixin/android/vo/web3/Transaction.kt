package one.mixin.android.vo.web3

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
class Transaction(
    @PrimaryKey
    @ColumnInfo("transaction_hash")
    val transactionHash: String,
    @ColumnInfo("raw_transaction")
    val rawTransaction: String,
    @ColumnInfo("chain_id")
    val chainId: String,
    @ColumnInfo("asset_key")
    val assetKey: String,
    @ColumnInfo("nonce")
    val nonce: Long,
    @ColumnInfo("created_at")
    val createdAt: String,
)