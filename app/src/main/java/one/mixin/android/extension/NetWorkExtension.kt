package one.mixin.android.extension

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import one.mixin.android.Constants
import timber.log.Timber

val autoDownloadPhoto: (value: Int) -> Boolean = {
    it.or(0x1110) == 0x1111
}
val autoDownloadAudio: (value: Int) -> Boolean = {
    it.or(0x1101) == 0x1111
}
val autoDownloadVideo: (value: Int) -> Boolean = {
    it.or(0x1011) == 0x1111
}
val autoDownloadDocument: (value: Int) -> Boolean = {
    it.or(0x0111) == 0x1111
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

fun Context.diffrentNetWorkAction(wifiAction: () -> Unit, mobileAction: () -> Unit, roaming: () -> Unit) {
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
    diffrentNetWorkAction({
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

fun Context.getAutoDownloadWifiValue() = defaultSharedPreferences.getInt(Constants.Download.AUTO_DOWNLOAD_WIFI, 0x1111)
fun Context.getAutoDownloadMobileValue() = defaultSharedPreferences.getInt(Constants.Download.AUTO_DOWNLOAD_MOBILE, 0x1000)
fun Context.getAutoDownloadRoamingValue() = defaultSharedPreferences.getInt(Constants.Download.AUTO_DOWNLOAD_ROAMING, 0x0000)

