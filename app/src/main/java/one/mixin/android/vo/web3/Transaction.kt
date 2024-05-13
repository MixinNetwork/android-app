package one.mixin.android.vo.web3

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions",
    primaryKeys = ["transaction_hash", "chain_id"]
)
class Transaction(
    @ColumnInfo("transaction_hash")
    val transactionHash: String,
    @ColumnInfo("chain_id")
    val chainId: String,
    @ColumnInfo("address")
    val address: String,
    @ColumnInfo("raw_transaction")
    val rawTransaction: String,
    @ColumnInfo("asset_key")
    val assetKey: String,
    @ColumnInfo("nonce")
    val nonce: Long,
    @ColumnInfo("created_at")
    val createdAt: String,
)