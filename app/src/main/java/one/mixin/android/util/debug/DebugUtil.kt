package one.mixin.android.util.debug

import android.view.View
import one.mixin.android.BuildConfig
import one.mixin.android.extension.tapVibrate
import timber.log.Timber
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun debugLongClick(view: View, debugAction: () -> Unit, releaseAction: (() -> Unit)? = null) {
    if (BuildConfig.DEBUG) {
        view.setOnLongClickListener {
            view.context.tapVibrate()
            debugAction.invoke()
            true
        }
    } else {
        view.setOnLongClickListener {
            view.context.tapVibrate()
            releaseAction?.invoke()
            true
        }
    }
}

inline fun <T> measureTimeMillis(tag: String, block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    val start = System.currentTimeMillis()
    val result = block()
    Timber.d("$tag ${System.currentTimeMillis() - start}")
    return result
}
