package one.mixin.android.vo.safe

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Treasury(
    @SerializedName("ratio")
    val ratio: String,
    @SerializedName("recipient")
    val recipient: String,
): Parcelable