package one.mixin.android.api.response

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Approval(
    val name: String,
    val symbol: String,
    @SerializedName("icon_url") val iconUrl: String,
    val sender: String,
    val amount: String
) : Parcelable
