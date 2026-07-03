package one.mixin.android.vo.safe

import android.os.Parcelable
import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.ColumnTypeConverters
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.db.converter.ListConverter

@Entity(tableName = "deposit_entries")
@ColumnTypeConverters(ListConverter::class)
@Parcelize
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
    @ColumnInfo(name = "members")
    @SerializedName("members")
    val members: List<String>,
    @ColumnInfo(name = "tag")
    @SerializedName("tag")
    val tag: String?,
    @ColumnInfo(name = "signature")
    @SerializedName("signature")
    val signature: String,
    @ColumnInfo(name = "threshold")
    @SerializedName("threshold")
    val threshold: Int,
    @ColumnInfo(name = "is_primary")
    @SerializedName("is_primary")
    val isPrimary: Boolean,
    @ColumnInfo(name = "minimum")
    @SerializedName("minimum")
    val minimum: String,
    @ColumnInfo(name = "maximum")
    @SerializedName("maximum")
    val maximum: String,
) : Parcelable
