package one.mixin.android.util

import android.os.Build
import androidx.collection.ArrayMap
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.sentry.Sentry
import io.sentry.SentryLevel
import one.mixin.android.BuildConfig
import one.mixin.android.extension.getStackTraceString
import one.mixin.android.session.Session

fun reportException(e: Throwable) {
    FirebaseCrashlytics.getInstance().recordException(e)
    Sentry.captureException(e)
}

fun reportException(
    msg: String,
    e: Throwable,
) {
    FirebaseCrashlytics.getInstance().log(msg + e.getStackTraceString())
    Sentry.captureMessage(msg + e.getStackTraceString())
}

fun reportEvent(msg: String) {
    Sentry.captureMessage(msg, SentryLevel.DEBUG)
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

fun Throwable.msg(): String {
    if (message == null) {
        reportEvent("Unknown exception: ${this.javaClass.name}")
        return this.javaClass.name
    }
    return message ?: "Unknown"
}