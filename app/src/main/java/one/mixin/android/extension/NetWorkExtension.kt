@file:Suppress("DEPRECATION")

package one.mixin.android.extension

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import one.mixin.android.Constants
import one.mixin.android.Constants.Download.MOBILE_DEFAULT
import one.mixin.android.Constants.Download.ROAMING_DEFAULT
import one.mixin.android.Constants.Download.WIFI_DEFAULT
import timber.log.Timber

val autoDownloadPhoto: (value: Int) -> Boolean = {
    it.or(0x110) == 0x111
}
val autoDownloadVideo: (value: Int) -> Boolean = {
    it.or(0x101) == 0x111
}
val autoDownloadDocument: (value: Int) -> Boolean = {
    it.or(0x011) == 0x111
}

fun Context.isConnectedToWiFi(): Boolean {
    try {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            ?: return false
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    } catch (e: Exception) {
        Timber.e(e)
    }
    return false
}

fun Context.isRoaming(): Boolean {
    try {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
                ?: return false
            return !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING)
        } else {
            val netInfo = connectivityManager.activeNetworkInfo
            if (netInfo != null) {
                return netInfo.isRoaming
            }
        }
    } catch (e: Exception) {
        Timber.e(e)
    }

    return false
}

fun Context.differentNetWorkAction(wifiAction: () -> Unit, mobileAction: () -> Unit, roaming: () -> Unit) {
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

fun Context.autoDownload(support: (value: Int) -> Boolean, action: () -> Unit) {
    differentNetWorkAction({
        if (support(getAutoDownloadWifiValue())) {
            action()
        }
    }, {
        if (support(getAutoDownloadMobileValue())) {
            action()
        }
    }, {
        if (support(getAutoDownloadRoamingValue())) {
            action()
        }
    })
}

fun Context.getAutoDownloadWifiValue() = defaultSharedPreferences.getInt(Constants.Download.AUTO_DOWNLOAD_WIFI, WIFI_DEFAULT)
fun Context.getAutoDownloadMobileValue() = defaultSharedPreferences.getInt(Constants.Download.AUTO_DOWNLOAD_MOBILE, MOBILE_DEFAULT)
fun Context.getAutoDownloadRoamingValue() = defaultSharedPreferences.getInt(Constants.Download.AUTO_DOWNLOAD_ROAMING, ROAMING_DEFAULT)
