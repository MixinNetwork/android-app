package one.mixin.android.db.web3.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.converter.AssetChangeListConverter

@Entity(
    tableName = "transactions",
    indices = [Index(value = arrayOf("transaction_at")), Index(value = arrayOf("transaction_type", "chain_id"))]
)
@Parcelize
data class Web3Transaction(
    @PrimaryKey
    @ColumnInfo(name = "transaction_hash")
    @SerializedName("transaction_hash")
    val transactionHash: String,

    @ColumnInfo(name = "transaction_type")
    @SerializedName("transaction_type")
    val transactionType: String,

    @ColumnInfo(name = "status")
    @SerializedName("status")
    val status: String,

    @ColumnInfo(name = "block_number")
    @SerializedName("block_number")
    val blockNumber: Long,

    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    val chainId: String,

    @ColumnInfo(name = "fee")
    @SerializedName("fee")
    val fee: String,

    @TypeConverters(AssetChangeListConverter::class)
    @ColumnInfo(name = "senders")
    @SerializedName("senders")
    val senders: List<AssetChange>?,

    @TypeConverters(AssetChangeListConverter::class)
    @ColumnInfo(name = "receivers")
    @SerializedName("receivers")
    val receivers: List<AssetChange>?,

    @TypeConverters(AssetChangeListConverter::class)
    @ColumnInfo(name = "approvals")
    @SerializedName("approvals")
    val approvals: List<AssetChange>? = null,

    @ColumnInfo(name = "send_asset_id")
    @SerializedName("send_asset_id")
    val sendAssetId: String? = null,

    @ColumnInfo(name = "receive_asset_id")
    @SerializedName("receive_asset_id")
    val receiveAssetId: String? = null,

    @ColumnInfo(name = "transaction_at")
    @SerializedName("transaction_at")
    val transactionAt: String,

    @ColumnInfo(name = "created_at")
    @SerializedName("created_at")
    val createdAt: String,

    @ColumnInfo(name = "updated_at")
    @SerializedName("updated_at")
    val updatedAt: String,
) : Parcelable

@Parcelize
data class AssetChange(
    @ColumnInfo(name = "asset_id")
    @SerializedName("asset_id")
    val assetId: String,
    
    @ColumnInfo(name = "amount")
    @SerializedName("amount")
    val amount: String,
    
    @ColumnInfo(name = "from")
    @SerializedName("from")
    val from: String? = null,
    
    @ColumnInfo(name = "to")
    @SerializedName("to")
    val to: String? = null
) : Parcelable
