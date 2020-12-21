package one.mixin.android.util.debug

import android.view.View
import one.mixin.android.BuildConfig
import one.mixin.android.extension.tapVibrate

fun debugLongClick(view: View, action: () -> Unit) {
    if (BuildConfig.DEBUG) {
        view.setOnLongClickListener {
            view.context.tapVibrate()
            action.invoke()
            true
        }
    }
}
