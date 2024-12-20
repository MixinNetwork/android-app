package one.mixin.android.util

import androidx.media3.common.PlaybackException
import androidx.media3.datasource.HttpDataSource
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.sentry.Sentry
import io.sentry.SentryLevel
import one.mixin.android.extension.getStackTraceString

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
