package one.mixin.android.vo

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import one.mixin.android.util.GsonHelper

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

fun checkLocationData(content: String): Boolean {
    return try {
        GsonHelper.customGson.fromJson(content, Location::class.java)
        true
    } catch (e: Exception) {
        false
    }
}

fun toLocationData(content: String): Location? {
    return try {
        GsonHelper.customGson.fromJson(content, Location::class.java)
    } catch (e: Exception) {
        null
    }
}
