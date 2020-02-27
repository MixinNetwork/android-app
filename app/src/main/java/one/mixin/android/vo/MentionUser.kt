package one.mixin.android.vo

import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName

data class MentionUser(
    @SerializedName(value = "identity_number", alternate = ["identityNumber"])
    @ColumnInfo(name = "identity_number")
    val identityNumber: String,
    @SerializedName(value = "full_name", alternate = ["fullName"])
    @ColumnInfo(name = "full_name")
    val fullName: String
)
