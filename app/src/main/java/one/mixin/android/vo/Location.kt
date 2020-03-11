package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@SuppressLint("ParcelCreator")
@Parcelize
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val address: String?,
    @SerializedName("icon_url")
    val iconUrl: String? = null
) : Parcelable
