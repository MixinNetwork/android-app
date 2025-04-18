package one.mixin.android.db.web3.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Ignore
import com.google.gson.annotations.SerializedName
import one.mixin.android.api.response.web3.BalanceChange
import one.mixin.android.api.response.web3.ParsedTx

@Entity(
    tableName = "raw_transactions",
    indices = [Index(value = arrayOf("chain_id"))],
)
data class Web3RawTransaction(
    @PrimaryKey
    @ColumnInfo(name = "hash") 
    val hash: String,

    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    val chainId: String,

    @ColumnInfo(name = "account")
    val account: String,

    @ColumnInfo(name = "nonce")
    val nonce: String,

    @ColumnInfo(name = "raw")
    val raw: String,

    @ColumnInfo(name = "state")
    val state: String,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    var updatedAt: String,
) {
    @Ignore
    @SerializedName("simulate_tx")
    var simulateTx: ParsedTx? = null
}