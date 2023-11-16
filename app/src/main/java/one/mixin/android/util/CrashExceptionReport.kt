package one.mixin.android.util

import android.os.Build
import androidx.collection.ArrayMap
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes
import one.mixin.android.BuildConfig
import one.mixin.android.extension.getStackTraceString
import one.mixin.android.session.Session

fun reportException(e: Throwable) {
    FirebaseCrashlytics.getInstance().recordException(e)
    Crashes.trackError(e)
}

fun reportException(
    msg: String,
    e: Throwable,
) {
    FirebaseCrashlytics.getInstance().log(msg + e.getStackTraceString())
    Crashes.trackError(
        e,
        ArrayMap<String, String>().apply {
            put("log", msg)
        },
        null,
    )
}

fun reportEvent(msg: String) {
    Analytics.trackEvent(
        msg,
        mutableMapOf<String, String>().apply {
            put("User ID", Session.getAccountId() ?: "")
            put("App Version", BuildConfig.VERSION_NAME)
            put("OS Version", Build.VERSION.SDK_INT.toString())
            put("Brand", Build.BRAND)
            put("Model", Build.MODEL)
        },
    )
}

fun reportExoPlayerException(
    prefix: String,
    error: PlaybackException,
) {
    val cause = error.cause
    if (cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 404) {
        return
    }
    val msg = "$prefix onPlayerError errorCode: ${error.errorCode}, errorCodeName: ${error.errorCodeName} cause: $cause"
    reportException(msg, error)
}
