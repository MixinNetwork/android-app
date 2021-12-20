package one.mixin.android.websocket

import android.annotation.SuppressLint
import android.os.Parcelable
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter
import one.mixin.android.util.reportException

@SuppressLint("ParcelCreator")
@Parcelize
@JsonClass(generateAdapter = true)
data class LocationPayload(
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val address: String?,
    @Json(name = "venue_type")
    val venueType: String? = null
) : Parcelable

fun LocationPayload.getImageUrl(): String? {
    if (venueType.isNullOrEmpty()) return null
    return "https://ss3.4sqi.net/img/categories_v2/${venueType}_88.png"
}

fun LocationPayload.toJson(): String =
    getTypeAdapter<LocationPayload>(LocationPayload::class.java).toJson(this)

fun checkLocationData(content: String): Boolean {
    return toLocationData(content) != null
}

fun toLocationData(content: String?): LocationPayload? {
    content ?: return null
    return try {
        getTypeAdapter<LocationPayload>(LocationPayload::class.java).fromJson(content)?.run {
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
