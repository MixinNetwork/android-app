@file:Suppress("DEPRECATION")

package one.mixin.android.extension

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
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
    if (hasWritePermission()) {
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
}

fun Context.getAutoDownloadWifiValue() = defaultSharedPreferences.getInt(Constants.Download.AUTO_DOWNLOAD_WIFI, WIFI_DEFAULT)
fun Context.getAutoDownloadMobileValue() = defaultSharedPreferences.getInt(Constants.Download.AUTO_DOWNLOAD_MOBILE, MOBILE_DEFAULT)
fun Context.getAutoDownloadRoamingValue() = defaultSharedPreferences.getInt(Constants.Download.AUTO_DOWNLOAD_ROAMING, ROAMING_DEFAULT)
