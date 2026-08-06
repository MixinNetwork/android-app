package one.mixin.android.extension

import android.util.Log

fun Throwable.getStackTraceString(): String = Log.getStackTraceString(this)
