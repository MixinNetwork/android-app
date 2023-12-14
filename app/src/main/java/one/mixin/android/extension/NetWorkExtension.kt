@file:Suppress("DEPRECATION")

package one.mixin.android.extension

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager
import one.mixin.android.Constants
import one.mixin.android.Constants.Download.MOBILE_DEFAULT
import one.mixin.android.Constants.Download.ROAMING_DEFAULT
import one.mixin.android.Constants.Download.WIFI_DEFAULT
import one.mixin.android.db.property.PropertyHelper
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

suspend fun Context.differentNetWorkAction(
    wifiAction: suspend () -> Unit,
    mobileAction: suspend () -> Unit,
    roaming: suspend () -> Unit,
) {
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

suspend fun Context.autoDownload(
    support: suspend (value: Int) -> Boolean,
    action: () -> Unit,
) {
    if (hasWritePermission() || Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
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
            },
        )
    }
}

suspend fun getAutoDownloadWifiValue() = PropertyHelper.findValueByKey(Constants.Download.AUTO_DOWNLOAD_WIFI, WIFI_DEFAULT)

suspend fun getAutoDownloadMobileValue() = PropertyHelper.findValueByKey(Constants.Download.AUTO_DOWNLOAD_MOBILE, MOBILE_DEFAULT)

suspend fun getAutoDownloadRoamingValue() = PropertyHelper.findValueByKey(Constants.Download.AUTO_DOWNLOAD_ROAMING, ROAMING_DEFAULT)

fun Context.networkType(): String {
    val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
    val nw = connectivityManager.activeNetwork ?: return "-"
    val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return "-"
    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
        else -> "?"
    }
}

fun Context.getNetworkOperatorName(): String {
    val manager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    return manager.networkOperatorName
}
