package one.mixin.android.vo.foursquare

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class FoursquareResponse(
    val venues: List<Venue>,
    val confident: Boolean
)
