package one.mixin.android.vo.safe

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import one.mixin.android.db.OutputDao
import one.mixin.android.vo.ListConverter

@TypeConverters(ListConverter::class)
@Entity(
    tableName = "outputs",
    indices = [
        Index(value = arrayOf("kernel_asset_id", "state", "created_at")),
        Index(value = arrayOf("transaction_hash", "output_index"), unique = true),
    ],
)
data class Output(
    @PrimaryKey
    @SerializedName("output_id")
    @ColumnInfo(name = "output_id")
    val outputId: String,
    @SerializedName("transaction_hash")
    @ColumnInfo(name = "transaction_hash")
    val transactionHash: String,
    @SerializedName("output_index")
    @ColumnInfo(name = "output_index")
    val outputIndex: Int,
    @SerializedName("kernel_asset_id")
    @ColumnInfo(name = "kernel_asset_id")
    val asset: String,
    @SerializedName("amount")
    @ColumnInfo(name = "amount")
    val amount: String,
    @SerializedName("mask")
    @ColumnInfo(name = "mask")
    val mask: String,
    @SerializedName("keys")
    @ColumnInfo(name = "keys")
    val keys: List<String>,
    @ColumnInfo(name = "threshold")
    val threshold: Int,
    @ColumnInfo(name = "extra")
    val extra: String,
    @ColumnInfo(name = "state")
    val state: String,
    @SerializedName("created_at")
    @ColumnInfo(name = "created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    @ColumnInfo(name = "updated_at")
    val updatedAt: String,
    @SerializedName("signed_by")
    @ColumnInfo(name = "signed_by")
    val signedBy: String,
    @SerializedName("signed_at")
    @ColumnInfo(name = "signed_at")
    val signedAt: String,
    @SerializedName("spent_at")
    @ColumnInfo(name = "spent_at")
    val spentAt: String,
)
