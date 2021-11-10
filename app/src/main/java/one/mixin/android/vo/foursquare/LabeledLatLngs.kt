package one.mixin.android.vo.foursquare

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class LabeledLatLngs(
    val label: String,
    val lat: Double,
    val lng: Double
)
