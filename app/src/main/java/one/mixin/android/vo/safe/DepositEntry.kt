package one.mixin.android.vo.safe

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import one.mixin.android.vo.ListConverter

@Entity(tableName = "deposit_entries")
@TypeConverters(ListConverter::class)
data class DepositEntry(
    @PrimaryKey
    @ColumnInfo(name = "entry_id")
    @SerializedName("entry_id")
    val entryId: String,
    @ColumnInfo(name = "chain_id")
    @SerializedName("chain_id")
    val chainId: String,
    @ColumnInfo(name = "destination")
    @SerializedName("destination")
    val destination: String,
    @ColumnInfo(name = "priority")
    @SerializedName("priority")
    val priority: Int,
    @ColumnInfo(name = "members")
    @SerializedName("members")
    val members: List<String>,
    @ColumnInfo(name = "tag")
    @SerializedName("tag")
    val tag: String?,
    @ColumnInfo(name="signature")
    @SerializedName("signature")
    val signature: String,
    @ColumnInfo(name = "threshold")
    @SerializedName("threshold")
    val threshold: Int,
)