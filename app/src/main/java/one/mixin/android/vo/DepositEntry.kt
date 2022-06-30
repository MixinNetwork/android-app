package one.mixin.android.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class DepositEntry(
    @SerializedName("destination")
    val destination: String,
    @SerializedName("tag")
    @ColumnInfo(name = "tag")
    val tag: String?,
    @SerializedName("properties")
    val properties: List<String>?
) : Parcelable
