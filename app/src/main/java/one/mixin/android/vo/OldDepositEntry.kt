package one.mixin.android.vo

import android.os.Parcelable
import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
class OldDepositEntry(
    @SerializedName("destination")
    @SerialName("destination")
    val destination: String,
    @SerializedName("tag")
    @ColumnInfo(name = "tag")
    @SerialName("tag")
    val tag: String?,
    @SerializedName("properties")
    @SerialName("properties")
    val properties: List<String>?,
) : Parcelable
