package one.mixin.android.util

import androidx.collection.ArrayMap
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.microsoft.appcenter.crashes.Crashes
import one.mixin.android.extension.getStackTraceString

fun reportException(e: Throwable) {
    FirebaseCrashlytics.getInstance().recordException(e)
    Crashes.trackError(e)
}

fun reportException(msg: String, e: Throwable) {
    FirebaseCrashlytics.getInstance().log(msg + e.getStackTraceString())
    Crashes.trackError(
        e,
        ArrayMap<String, String>().apply {
            put("log", msg)
        },
        null
    )
}

fun reportLog(msg: String) {
    FirebaseCrashlytics.getInstance().log(msg)
    Crashes.trackError(
        Exception(),
        ArrayMap<String, String>().apply {
            put("log", msg)
        },
        null
    )
}

fun reportExoPlayerException(prefix: String, error: PlaybackException) {
    val cause = error.cause
    if (cause is HttpDataSource.InvalidResponseCodeException && cause.responseCode == 404) {
        return
    }
    val msg = "$prefix onPlayerError errorCode: ${error.errorCode}, errorCodeName: ${error.errorCodeName} cause: $cause"
    reportException(msg, error)
}
