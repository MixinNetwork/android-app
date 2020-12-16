package one.mixin.android.util.debug

import android.content.ClipData
import android.view.View
import one.mixin.android.BuildConfig
import one.mixin.android.extension.getClipboardManager
import one.mixin.android.extension.tapVibrate
import one.mixin.android.extension.vibrate

fun debugLongClick(view: View,action:()->Unit){
    if (BuildConfig.DEBUG) {
        view.setOnLongClickListener {
            view.context.tapVibrate()
            action.invoke()
            true
        }
    }
}