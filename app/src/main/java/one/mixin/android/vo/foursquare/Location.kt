package one.mixin.android.vo.foursquare

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class Location(
    val address: String?,
    val lat: Double,
    val lng: Double,
    val labeledLatLngs: List<LabeledLatLngs>?,
    val distance: Int,
    val cc: String?,
    val neighborhood: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val formattedAddress: List<String>?
)
