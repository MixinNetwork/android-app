@file:Suppress("DEPRECATION")

package one.mixin.android.extension

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import one.mixin.android.Constants
import one.mixin.android.Constants.Download.MOBILE_DEFAULT
import one.mixin.android.Constants.Download.ROAMING_DEFAULT
import one.mixin.android.Constants.Download.WIFI_DEFAULT
import one.mixin.android.util.PropertyHelper
import timber.log.Timber

val autoDownloadPhoto: suspend (value: Int) -> Boolean = {
    it.or(0x110) == 0x111
}
val autoDownloadVideo: suspend (value: Int) -> Boolean = {
    it.or(0x101) == 0x111
}
val autoDownloadDocument: suspend (value: Int) -> Boolean = {
    it.or(0x011) == 0x111
}

fun Context.isConnectedToWiFi(): Boolean {
    try {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (netInfo != null && netInfo.state == NetworkInfo.State.CONNECTED) {
            return true
        }
    } catch (e: Exception) {
        Timber.e(e)
    }

    return false
}

fun Context.isRoaming(): Boolean {
    try {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = connectivityManager.activeNetworkInfo
        if (netInfo != null) {
            return netInfo.isRoaming
        }
    } catch (e: Exception) {
        Timber.e(e)
    }

    return false
}

suspend fun Context.differentNetWorkAction(wifiAction: suspend () -> Unit, mobileAction: suspend () -> Unit, roaming: suspend () -> Unit) {
    when {
        isConnectedToWiFi() -> {
            wifiAction()
        }
        isRoaming() -> {
            roaming()
        }
        else -> {
            mobileAction()
        }
    }
}

suspend fun Context.autoDownload(support: suspend (value: Int) -> Boolean, action: () -> Unit) {
    if (hasWritePermission()) {
        differentNetWorkAction(
            {
                if (support(getAutoDownloadWifiValue())) {
                    action()
                }
            },
            {
                if (support(getAutoDownloadMobileValue())) {
                    action()
                }
            },
            {
                if (support(getAutoDownloadRoamingValue())) {
                    action()
                }
            }
        )
    }
}

suspend fun getAutoDownloadWifiValue() = PropertyHelper.findValueByKey(Constants.Download.AUTO_DOWNLOAD_WIFI)?.toIntOrNull() ?: WIFI_DEFAULT
suspend fun getAutoDownloadMobileValue() = PropertyHelper.findValueByKey(Constants.Download.AUTO_DOWNLOAD_MOBILE)?.toIntOrNull() ?: MOBILE_DEFAULT
suspend fun getAutoDownloadRoamingValue() = PropertyHelper.findValueByKey(Constants.Download.AUTO_DOWNLOAD_ROAMING)?.toIntOrNull() ?: ROAMING_DEFAULT

@SuppressLint("MissingPermission")
fun Context.networkType(): String {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (manager != null) {
        val networkInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (networkInfo != null && networkInfo.isConnectedOrConnecting) {
            return "WIFI"
        }
    }
    val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        ?: return "Other"
    return when (telephonyManager.networkType) {
        TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
        TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
        TelephonyManager.NETWORK_TYPE_LTE -> "4G"
        else -> "Other"
    }
}

fun Context.getNetworkOperatorName(): String {
    val manager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    return manager.networkOperatorName
}
