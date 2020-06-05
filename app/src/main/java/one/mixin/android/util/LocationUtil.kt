package one.mixin.android.util

import com.google.android.gms.maps.model.LatLng
import one.mixin.android.R
import java.lang.Math.toRadians
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

fun calculationByDistance(StartP: LatLng, EndP: LatLng): Double {
    val radius = 6371 // radius of earth in Km
    val lat1 = StartP.latitude
    val lat2 = EndP.latitude
    val lon1 = StartP.longitude
    val lon2 = EndP.longitude
    val dLat = toRadians(lat2 - lat1)
    val dLon = toRadians(lon2 - lon1)
    val a = (
        sin(dLat / 2) * sin(dLat / 2) +
            (
                cos(toRadians(lat1)) *
                    cos(toRadians(lat2)) * sin(dLon / 2) *
                    sin(dLon / 2)
                )
        )
    val c = 2 * asin(sqrt(a))
    return radius * c
}

fun Double.distanceFormat(): Pair<String, Int> {
    return if (this < 1) {
        Pair(String.format("%d", (this * 1000).toInt()), R.string.location_metre)
    } else {
        Pair(String.format("%.2f", this), R.string.location_kilometre)
    }
}
