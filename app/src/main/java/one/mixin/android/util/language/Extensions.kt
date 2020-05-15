package one.mixin.android.util.language

import android.app.Activity
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_META_DATA
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

@Suppress("DEPRECATION")
internal fun Configuration.getLocaleCompat(): Locale {
    return if (isAtLeastSdkVersion(Build.VERSION_CODES.N)) locales.get(0) else locale
}

internal fun isAtLeastSdkVersion(versionCode: Int): Boolean {
    return Build.VERSION.SDK_INT >= versionCode
}

internal fun Activity.resetTitle() {
    try {
        val info = packageManager.getActivityInfo(componentName, GET_META_DATA)
        if (info.labelRes != 0) {
            setTitle(info.labelRes)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
    }
}
