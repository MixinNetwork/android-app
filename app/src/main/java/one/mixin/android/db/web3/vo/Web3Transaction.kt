package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Entity(
    tableName = "transactions",
    indices = [Index(value = arrayOf("transaction_at")), Index(value = arrayOf("transaction_type", "asset_id"))]
)
@Parcelize
data class Web3Transaction(
    @PrimaryKey
    @ColumnInfo(name = "transaction_id")
    @SerializedName("transaction_id")
    val id: String,

    @ColumnInfo(name = "transaction_type")
    @SerializedName("transaction_type")
    val transactionType: String,

    @ColumnInfo(name = "transaction_hash")
    @SerializedName("transaction_hash")
    val transactionHash: String,

    @ColumnInfo(name = "block_number")
    @SerializedName("block_number")
    val blockNumber: Long,

    @ColumnInfo(name = "sender")
    @SerializedName("sender")
    val sender: String,

    @ColumnInfo(name = "receiver")
    @SerializedName("receiver")
    val receiver: String,

    @ColumnInfo(name = "output_hash")
    @SerializedName("output_hash")
    val outputHash: String,

    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    val chainId: String,

    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,

    @ColumnInfo(name = "amount")
    @SerializedName("amount")
    val amount: String,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String,

    @ColumnInfo(name = "transaction_at")
    @SerializedName("transaction_at")
    val transactionAt: String,

    @ColumnInfo(name = "status")
    @SerializedName("status")
    val status: String,
) : Parcelable
