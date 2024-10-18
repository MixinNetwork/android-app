package one.mixin.android.vo

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
class Dapp(
    @SerializedName("name")
    val name: String,
    @SerializedName("home_url")
    val homeUrl: String,
    @SerializedName("chains")
    val chains: List<String>,
    @SerializedName("icon_url")
    val iconUrl: String,
    @SerializedName("category")
    val category: String,
): Parcelable
