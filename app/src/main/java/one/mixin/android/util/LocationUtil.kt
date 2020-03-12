package one.mixin.android.util

import com.google.android.gms.maps.model.LatLng
import java.lang.Math.PI
import java.lang.Math.abs
import java.lang.Math.asin
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import java.lang.Math.toRadians
import java.text.DecimalFormat
import one.mixin.android.R

fun calculationByDistance(StartP: LatLng, EndP: LatLng): Double {
    val Radius = 6371 // radius of earth in Km
    val lat1 = StartP.latitude
    val lat2 = EndP.latitude
    val lon1 = StartP.longitude
    val lon2 = EndP.longitude
    val dLat = toRadians(lat2 - lat1)
    val dLon = toRadians(lon2 - lon1)
    val a = (sin(dLat / 2) * sin(dLat / 2) +
        (cos(toRadians(lat1)) *
            cos(toRadians(lat2)) * sin(dLon / 2) *
            sin(dLon / 2)))
    val c = 2 * asin(sqrt(a))
    val valueResult = Radius * c
    val km = valueResult / 1
    val newFormat = DecimalFormat("####")
    val kmInDec: Int = Integer.valueOf(newFormat.format(km))
    val meter = valueResult % 1000
    val meterInDec: Int = Integer.valueOf(newFormat.format(meter))

    return Radius * c
}

fun Double.distanceFormat(): Pair<String, Int> {
    return if (this < 1) {
        Pair(String.format("%d", (this * 1000).toInt()), R.string.location_metre)
    } else {
        Pair(String.format("%.2f", this), R.string.location_kilometre)
    }
}

object JZLocationConverter {
    private fun LAT_OFFSET_0(x: Double, y: Double): Double {
        return -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
    }

    private fun LAT_OFFSET_1(x: Double, y: Double): Double {
        return (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
    }

    private fun LAT_OFFSET_2(x: Double, y: Double): Double {
        return (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
    }

    private fun LAT_OFFSET_3(x: Double, y: Double): Double {
        return (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
    }

    private fun LON_OFFSET_0(x: Double, y: Double): Double {
        return 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
    }

    private fun LON_OFFSET_1(x: Double, y: Double): Double {
        return (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
    }

    private fun LON_OFFSET_2(x: Double, y: Double): Double {
        return (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
    }

    private fun LON_OFFSET_3(x: Double, y: Double): Double {
        return (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
    }

    private const val RANGE_LON_MAX = 137.8347
    private const val RANGE_LON_MIN = 72.004
    private const val RANGE_LAT_MAX = 55.8271
    private const val RANGE_LAT_MIN = 0.8293
    private const val jzA = 6378245.0
    private const val jzEE = 0.00669342162296594323
    fun transformLat(x: Double, y: Double): Double {
        var ret = LAT_OFFSET_0(x, y)
        ret += LAT_OFFSET_1(x, y)
        ret += LAT_OFFSET_2(x, y)
        ret += LAT_OFFSET_3(x, y)
        return ret
    }

    fun transformLon(x: Double, y: Double): Double {
        var ret = LON_OFFSET_0(x, y)
        ret += LON_OFFSET_1(x, y)
        ret += LON_OFFSET_2(x, y)
        ret += LON_OFFSET_3(x, y)
        return ret
    }

    fun outOfChina(lat: Double, lon: Double): Boolean {
        if (lon < RANGE_LON_MIN || lon > RANGE_LON_MAX) return true
        return if (lat < RANGE_LAT_MIN || lat > RANGE_LAT_MAX) true else false
    }

    fun gcj02Encrypt(ggLat: Double, ggLon: Double): LatLng {
        val resPoint = LatLng()
        val mgLat: Double
        val mgLon: Double
        if (outOfChina(ggLat, ggLon)) {
            resPoint.latitude = ggLat
            resPoint.longitude = ggLon
            return resPoint
        }
        var dLat = transformLat(ggLon - 105.0, ggLat - 35.0)
        var dLon = transformLon(ggLon - 105.0, ggLat - 35.0)
        val radLat = ggLat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - jzEE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = dLat * 180.0 / (jzA * (1 - jzEE) / (magic * sqrtMagic) * PI)
        dLon = dLon * 180.0 / (jzA / sqrtMagic * cos(radLat) * PI)
        mgLat = ggLat + dLat
        mgLon = ggLon + dLon
        resPoint.latitude = mgLat
        resPoint.longitude = mgLon
        return resPoint
    }

    fun gcj02Decrypt(gjLat: Double, gjLon: Double): LatLng {
        val gPt = gcj02Encrypt(gjLat, gjLon)
        val dLon = gPt.longitude - gjLon
        val dLat = gPt.latitude - gjLat
        val pt = LatLng()
        pt.latitude = gjLat - dLat
        pt.longitude = gjLon - dLon
        return pt
    }

    fun bd09Decrypt(bdLat: Double, bdLon: Double): LatLng {
        val gcjPt = LatLng()
        val x = bdLon - 0.0065
        val y = bdLat - 0.006
        val z = sqrt(x * x + y * y) - 0.00002 * sin(y * PI)
        val theta = atan2(y, x) - 0.000003 * cos(x * PI)
        gcjPt.longitude = z * cos(theta)
        gcjPt.latitude = z * sin(theta)
        return gcjPt
    }

    fun bd09Encrypt(ggLat: Double, ggLon: Double): LatLng {
        val bdPt = LatLng()
        val z = sqrt(ggLon * ggLon + ggLat * ggLat) + 0.00002 * sin(ggLat * PI)
        val theta = atan2(ggLat, ggLon) + 0.000003 * cos(ggLon * PI)
        bdPt.longitude = z * cos(theta) + 0.0065
        bdPt.latitude = z * sin(theta) + 0.006
        return bdPt
    }

    /**
     * @param location 世界标准地理坐标(WGS-84)
     * @return 中国国测局地理坐标（GCJ-02）<火星坐标>
     * @brief 世界标准地理坐标(WGS-84) 转换成 中国国测局地理坐标（GCJ-02）<火星坐标>
     *
     * ####只在中国大陆的范围的坐标有效，以外直接返回世界标准坐标
    </火星坐标></火星坐标> */
    fun wgs84ToGcj02(location: LatLng): LatLng {
        return gcj02Encrypt(location.latitude, location.longitude)
    }

    /**
     * @param location 中国国测局地理坐标（GCJ-02）
     * @return 世界标准地理坐标（WGS-84）
     * @brief 中国国测局地理坐标（GCJ-02） 转换成 世界标准地理坐标（WGS-84）
     *
     * ####此接口有1－2米左右的误差，需要精确定位情景慎用
     */
    fun gcj02ToWgs84(location: LatLng): LatLng {
        return gcj02Decrypt(location.latitude, location.longitude)
    }

    /**
     * @param location 世界标准地理坐标(WGS-84)
     * @return 百度地理坐标（BD-09)
     * @brief 世界标准地理坐标(WGS-84) 转换成 百度地理坐标（BD-09)
     */
    fun wgs84ToBd09(location: LatLng): LatLng {
        val gcj02Pt = gcj02Encrypt(location.latitude, location.longitude)
        return bd09Encrypt(gcj02Pt.latitude, gcj02Pt.longitude)
    }

    /**
     * @param location 中国国测局地理坐标（GCJ-02）<火星坐标>
     * @return 百度地理坐标（BD-09)
     * @brief 中国国测局地理坐标（GCJ-02）<火星坐标> 转换成 百度地理坐标（BD-09)
    </火星坐标></火星坐标> */
    fun gcj02ToBd09(location: LatLng): LatLng {
        return bd09Encrypt(location.latitude, location.longitude)
    }

    /**
     * @param location 百度地理坐标（BD-09)
     * @return 中国国测局地理坐标（GCJ-02）<火星坐标>
     * @brief 百度地理坐标（BD-09) 转换成 中国国测局地理坐标（GCJ-02）<火星坐标>
    </火星坐标></火星坐标> */
    fun bd09ToGcj02(location: LatLng): LatLng {
        return bd09Decrypt(location.latitude, location.longitude)
    }

    /**
     * @param location 百度地理坐标（BD-09)
     * @return 世界标准地理坐标（WGS-84）
     * @brief 百度地理坐标（BD-09) 转换成 世界标准地理坐标（WGS-84）
     *
     * ####此接口有1－2米左右的误差，需要精确定位情景慎用
     */
    fun bd09ToWgs84(location: LatLng): LatLng {
        val gcj02 = bd09ToGcj02(location)
        return gcj02Decrypt(gcj02.latitude, gcj02.longitude)
    }

    class LatLng(var latitude: Double = 0.0, var longitude: Double = 0.0)
}
