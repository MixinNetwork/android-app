package one.mixin.android.util.debug

import android.database.Cursor
import android.view.View
import androidx.core.database.getBlobOrNull
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getFloatOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getShortOrNull
import androidx.core.database.getStringOrNull
import one.mixin.android.BuildConfig
import one.mixin.android.extension.tapVibrate
import timber.log.Timber
import java.lang.Exception
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
    Timber.d("tag ${System.currentTimeMillis() - start}")
    return result
}

fun Cursor.getContent(columnIndex: Int): String {
    return try {
        (
            getStringOrNull(columnIndex)
                ?: getIntOrNull(columnIndex)
                ?: getBlobOrNull(columnIndex)?.contentToString()
                ?: getFloatOrNull(columnIndex)
                ?: getDoubleOrNull(columnIndex)
                ?: getShortOrNull(columnIndex)
                ?: getLongOrNull(columnIndex) ?: ""
            ).toString()
    } catch (e: Exception) {
        ""
    }
}
