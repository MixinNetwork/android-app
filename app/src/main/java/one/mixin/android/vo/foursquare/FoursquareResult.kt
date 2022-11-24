package one.mixin.android.vo.foursquare

class FoursquareResult(
    val meta: Meta,
    val response: FoursquareResponse?,
) {
    fun isSuccess(): Boolean {
        return meta.code == 200 && response != null
    }
}
