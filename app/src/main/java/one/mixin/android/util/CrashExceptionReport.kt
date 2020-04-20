package one.mixin.android.util

import com.bugsnag.android.Bugsnag
import com.google.firebase.crashlytics.FirebaseCrashlytics

fun reportException(e: Throwable) {
    Bugsnag.notify(e)
    FirebaseCrashlytics.getInstance().recordException(e)
}
