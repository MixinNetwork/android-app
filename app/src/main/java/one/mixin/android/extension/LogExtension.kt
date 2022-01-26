package one.mixin.android.extension

import android.util.Log

inline fun Throwable.getStackTraceString(): String = Log.getStackTraceString(this)
