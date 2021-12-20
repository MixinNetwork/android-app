package one.mixin.android.vo.foursquare

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class FoursquareResult(
    val meta: Meta,
    val response: FoursquareResponse?
) {
    fun isSuccess(): Boolean {
        return meta.code == 200 && response != null
    }
}
