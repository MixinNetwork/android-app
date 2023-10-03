package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName

@TypeConverters(ListConverter::class)
@Entity(tableName = "outputs")
class Output(
    @PrimaryKey
    @SerializedName("utxo_id")
    @ColumnInfo(name = "utxo_id")
    val utxoId: String,
    @ColumnInfo(name = "type")
    val type: String,
    @SerializedName("user_id")
    @ColumnInfo(name = "user_id")
    val userId: String,
    @SerializedName("asset_id")
    @ColumnInfo(name = "asset_id")
    val assetId: String,
    @SerializedName("transaction_hash")
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String,
    @SerializedName("output_index")
    @ColumnInfo(name = "output_index")
    val outputIndex: Int,
    @ColumnInfo(name = "amount")
    val amount: String,
    @SerializedName("members")
    @ColumnInfo(name = "members")
    val members: List<String>,
    @ColumnInfo(name = "threshold")
    val threshold: Int,
    @ColumnInfo(name = "memo")
    val memo: String?,
    @ColumnInfo(name = "state")
    val state: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
)
