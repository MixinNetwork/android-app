package one.mixin.android.websocket

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize
import one.mixin.android.util.GsonHelper

@SuppressLint("ParcelCreator")
@Parcelize
data class LocationPayload(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val address: String?,
    @SerializedName("venue_type")
    val venueType: String? = null
) : Parcelable

fun LocationPayload.getImageUrl(): String? {
    if (venueType.isNullOrEmpty()) return null
    return "https://ss3.4sqi.net/img/categories_v2/${venueType}_88.png"
}

fun checkLocationData(content: String): Boolean {
    return try {
        GsonHelper.customGson.fromJson(content, LocationPayload::class.java)
        true
    } catch (e: Exception) {
        false
    }
}

fun toLocationData(content: String): LocationPayload? {
    return try {
        GsonHelper.customGson.fromJson(content, LocationPayload::class.java)
    } catch (e: Exception) {
        null
    }
}
