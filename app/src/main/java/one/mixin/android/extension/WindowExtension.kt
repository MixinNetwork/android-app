package one.mixin.android.extension

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowInsets
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.ActivityEmbeddingController
import androidx.window.layout.SupportedPosture
import androidx.window.layout.WindowInfoTracker
import androidx.window.layout.WindowMetricsCalculator
import one.mixin.android.ui.common.BaseActivity

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

fun Context.windowWidth(): Int =
    WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds.width()

fun Context.windowHeight(): Int =
    WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds.height()

fun RecyclerView.refreshHoldersOnWidthChange() {
    var wasCompact = context.isCompactMessageWindow()
    var lastWindowHeight = context.windowHeight()
    addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
        val compact = context.isCompactMessageWindow()
        val skipRefresh = wasCompact && compact
        wasCompact = compact
        val windowHeight = if (skipRefresh) lastWindowHeight else context.windowHeight()
        val heightChanged = windowHeight != lastWindowHeight
        lastWindowHeight = windowHeight
        if (!skipRefresh && oldRight > oldLeft && (right - left != oldRight - oldLeft || heightChanged)) {
            post {
                if (!isAttachedToWindow) return@post
                val state = layoutManager?.onSaveInstanceState()
                // Holders cache bubble dimensions at construction, so a rebind alone is insufficient.
                swapAdapter(adapter, true)
                recycledViewPool.clear()
                layoutManager?.onRestoreInstanceState(state)
            }
        }
    }
}

fun Context.contentWidth(): Int {
    val content = findFragmentActivityOrNull()?.findViewById<View>(android.R.id.content)
    return content?.let { (it.width - it.paddingLeft - it.paddingRight).takeIf { width -> width > 0 } } ?: windowWidth()
}

fun Context.isActivityEmbedded(): Boolean = findFragmentActivityOrNull()?.let {
    ActivityEmbeddingController.getInstance(it).isActivityEmbedded(it)
} ?: false

internal fun Context.isPhoneLandscape(): Boolean {
    val activity = findFragmentActivityOrNull() ?: return false
    if (activity.resources.configuration.smallestScreenWidthDp >= 600 || activity.isActivityEmbedded() ||
        activity.isInMultiWindowMode || activity.isInPictureInPictureMode ||
        (activity as? BaseActivity)?.hasObservedFold == true
    ) return false
    val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(activity).bounds
    if (bounds.width() <= bounds.height() || minOf(bounds.width(), bounds.height()) / resources.displayMetrics.density >= 600f) return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && activity.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_HINGE_ANGLE)) return false
    if (WindowSdkExtensions.getInstance().extensionVersion < 6) return false
    return try {
        SupportedPosture.TABLETOP !in WindowInfoTracker.getOrCreate(activity).supportedPostures
    } catch (_: UnsupportedOperationException) {
        false
    }
}

// The current window's short side, so phones in either orientation and a folded cover screen stay compact.
fun Context.isCompactWindow(): Boolean {
    val bounds = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(this).bounds
    return minOf(bounds.width(), bounds.height()) / resources.displayMetrics.density < 600f
}
