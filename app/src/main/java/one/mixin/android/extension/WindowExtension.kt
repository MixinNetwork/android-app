package one.mixin.android.extension

import android.os.Build
import android.view.WindowInsets

fun WindowInsets.getSystemWindowTop(): Int {
    @Suppress("DEPRECATION")
    return systemWindowInsetTop

//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).top
//    } else {
//        @Suppress("DEPRECATION")
//        systemWindowInsetTop
//    }
}

fun WindowInsets.getSystemWindowBottom(): Int {
    @Suppress("DEPRECATION")
    return systemWindowInsetBottom

//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//        getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).bottom + getInsets(WindowInsets.Type.ime()).bottom
//    } else {
//        @Suppress("DEPRECATION")
//        systemWindowInsetBottom
//    }
}

fun WindowInsets.getSystemWindowLeft(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).left
    } else {
        @Suppress("DEPRECATION")
        systemWindowInsetLeft
    }
}

fun WindowInsets.getSystemWindowRight(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).right
    } else {
        @Suppress("DEPRECATION")
        systemWindowInsetRight
    }
}
