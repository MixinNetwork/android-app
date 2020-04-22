package one.mixin.android.util

import com.bugsnag.android.Bugsnag
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.jetbrains.anko.getStackTraceString

fun reportException(e: Throwable) {
    Bugsnag.notify(e)
    FirebaseCrashlytics.getInstance().recordException(e)
}

fun reportException(msg: String, e: Throwable) {
    Bugsnag.notify(e)
    FirebaseCrashlytics.getInstance().log(msg + e.getStackTraceString())
}
