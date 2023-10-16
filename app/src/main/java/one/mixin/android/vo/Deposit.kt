package one.mixin.android.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName

@Entity(tableName = "deposits")
@TypeConverters(ListConverter::class)
data class Deposit(
    @PrimaryKey
    @ColumnInfo(name = "entry_id")
    @SerializedName("entry_id")
    val entryId: String,
    @ColumnInfo(name = "threshold")
    @SerializedName("threshold")
    val threshold: Int,
    @ColumnInfo(name = "destination")
    @SerializedName("destination")
    val destination: String,
    @ColumnInfo(name = "tag")
    @SerializedName("tag")
    val tag: String,
    @ColumnInfo(name = "chainId")
    @SerializedName("chain_id")
    val chainId: String,
    @ColumnInfo(name = "assetId")
    @SerializedName("asset_id")
    val assetId: String,
    @ColumnInfo(name = "priority")
    @SerializedName("priority")
    val priority: Int,
    @ColumnInfo(name = "members")
    @SerializedName("members")
    val members: List<String>,
)