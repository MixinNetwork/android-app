package one.mixin.android.websocket

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.reportException

@SuppressLint("ParcelCreator")
@Parcelize
data class LocationPayload(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("name")
    val name: String?,
    @SerializedName("address")
    val address: String?,
    @SerializedName("venue_type")
    val venueType: String? = null,
) : Parcelable

fun LocationPayload.getImageUrl(): String? {
    if (venueType.isNullOrEmpty()) return null
    return "https://ss3.4sqi.net/img/categories_v2/${venueType}_88.png"
}

fun checkLocationData(content: String): Boolean {
    return toLocationData(content) != null
}

fun toLocationData(content: String?): LocationPayload? {
    content ?: return null
    return try {
        GsonHelper.customGson.fromJson(content, LocationPayload::class.java).run {
            if (latitude == 0.0 && longitude == 0.0) {
                return null
            }
            this
        }
    } catch (e: java.lang.Exception) {
        FirebaseCrashlytics.getInstance().log("LocationHolder decrypt failed $content")
        reportException(e)
        null
    }
}
