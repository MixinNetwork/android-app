package one.mixin.android.util

import androidx.collection.ArrayMap
import com.bugsnag.android.Bugsnag
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.microsoft.appcenter.crashes.Crashes
import org.jetbrains.anko.getStackTraceString

fun reportException(e: Throwable) {
    Bugsnag.notify(e)
    FirebaseCrashlytics.getInstance().recordException(e)
    Crashes.trackError(e)
}

fun reportException(msg: String, e: Throwable) {
    Bugsnag.notify(e)
    FirebaseCrashlytics.getInstance().log(msg + e.getStackTraceString())
    Crashes.trackError(e, ArrayMap<String, String>().apply {
        put("log", msg)
    }, null)
}
