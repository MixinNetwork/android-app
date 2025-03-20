package one.mixin.android.db.web3.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "raw_transactions",
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
    var updatedAt: String
)