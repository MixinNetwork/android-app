package one.mixin.android.util

import android.app.Activity
import android.graphics.Rect
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

internal fun tabletopFoldBounds(feature: FoldingFeature?, container: Rect, minimumPaneSize: Int): Rect? {
    if (feature == null || feature.orientation != FoldingFeature.Orientation.HORIZONTAL) return null
    if (feature.state != FoldingFeature.State.HALF_OPENED && !feature.isSeparating && feature.occlusionType != FoldingFeature.OcclusionType.FULL) return null
    val fold = feature.bounds
    if (fold.left > container.left || fold.right < container.right) return null
    if (fold.top - container.top < minimumPaneSize || container.bottom - fold.bottom < minimumPaneSize) return null
    return Rect(0, fold.top - container.top, container.width(), fold.bottom - container.top)
}

fun LifecycleOwner.observeTabletopFold(activity: Activity, view: View, minimumPaneSize: Int, onChanged: (Rect?) -> Unit): Job =
    observeFolds(activity, view) { features, container ->
        onChanged(tabletopFoldBounds(features.firstOrNull { it.orientation == FoldingFeature.Orientation.HORIZONTAL }, container, minimumPaneSize))
    }

internal fun separatingFoldPadding(feature: FoldingFeature?, container: Rect): Rect {
    val padding = Rect()
    if (feature == null || (!isTabletopFold(feature) && !feature.isSeparating && feature.occlusionType != FoldingFeature.OcclusionType.FULL)) return padding
    val fold = feature.bounds
    if (feature.orientation == FoldingFeature.Orientation.VERTICAL && fold.top <= container.top && fold.bottom >= container.bottom && fold.left > container.left && fold.right < container.right) {
        if (fold.left - container.left >= container.right - fold.right) padding.right = container.right - fold.left
        else padding.left = fold.right - container.left
    } else if (feature.orientation == FoldingFeature.Orientation.HORIZONTAL && isTabletopFold(feature) && fold.left <= container.left && fold.right >= container.right && fold.top > container.top && fold.bottom < container.bottom) {
        // Flip phones fold horizontally. Keep the page above the crease instead of
        // jumping to whichever half is larger.
        padding.bottom = container.bottom - fold.top
    }
    return padding
}

internal data class BottomSheetFoldFrame(
    val left: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val maxHeight: Int,
)

internal fun bottomSheetFoldFrame(feature: FoldingFeature?, container: Rect, minimumPane: Int): BottomSheetFoldFrame {
    val fullHeight = container.height().coerceAtLeast(0)
    val unchanged = BottomSheetFoldFrame(maxHeight = fullHeight)
    if (feature == null) return unchanged
    val fold = feature.bounds
    if (feature.orientation == FoldingFeature.Orientation.VERTICAL &&
        (feature.isSeparating || feature.occlusionType == FoldingFeature.OcclusionType.FULL) &&
        fold.top <= container.top && fold.bottom >= container.bottom &&
        fold.left > container.left && fold.right < container.right
    ) {
        val padding = separatingFoldPadding(feature, container)
        return BottomSheetFoldFrame(left = padding.left, right = padding.right, maxHeight = fullHeight)
    }
    if (!isTabletopFold(feature) || fold.left > container.left || fold.right < container.right || fold.top <= container.top || fold.bottom >= container.bottom) {
        return unchanged
    }
    val below = container.bottom - fold.bottom
    val above = fold.top - container.top
    return if (below >= minimumPane) {
        BottomSheetFoldFrame(maxHeight = below)
    } else if (above >= minimumPane) {
        BottomSheetFoldFrame(bottom = container.bottom - fold.top, maxHeight = above)
    } else {
        unchanged
    }
}

private fun isTabletopFold(feature: FoldingFeature): Boolean =
    feature.orientation == FoldingFeature.Orientation.HORIZONTAL &&
        (feature.state == FoldingFeature.State.HALF_OPENED || feature.isSeparating)

fun LifecycleOwner.avoidSeparatingFolds(activity: Activity, view: View, allowTabletop: Boolean = false): Job {
    val original = Rect(view.paddingLeft, view.paddingTop, view.paddingRight, view.paddingBottom)
    return observeFolds(activity, view) { features, container ->
        val feature = features.firstOrNull { feature ->
            val horizontal = feature.orientation == FoldingFeature.Orientation.HORIZONTAL
            if (allowTabletop && horizontal) return@firstOrNull false
            (horizontal && isTabletopFold(feature)) || feature.isSeparating || feature.occlusionType == FoldingFeature.OcclusionType.FULL
        }
        val padding = separatingFoldPadding(feature, container)
        view.setPadding(original.left + padding.left, original.top + padding.top, original.right + padding.right, original.bottom + padding.bottom)
    }.also { job ->
        job.invokeOnCompletion { view.setPadding(original.left, original.top, original.right, original.bottom) }
    }
}

fun LifecycleOwner.avoidBottomSheetFolds(activity: Activity, container: View, allowTabletop: Boolean = false): Job {
    val original = Rect(container.paddingLeft, container.paddingTop, container.paddingRight, container.paddingBottom)
    val minimumPane = (160 * activity.resources.displayMetrics.density).toInt()
    return observeFolds(activity, container) { features, bounds ->
        val feature = features.firstOrNull {
            !allowTabletop || it.orientation == FoldingFeature.Orientation.VERTICAL
        }
        val frame = bottomSheetFoldFrame(feature, bounds, minimumPane)
        val top = if (frame.bottom == 0 && frame.maxHeight < bounds.height()) bounds.height() - frame.maxHeight else 0
        container.setPadding(original.left + frame.left, original.top + top, original.right + frame.right, original.bottom + frame.bottom)
    }.also { job ->
        job.invokeOnCompletion {
            container.setPadding(original.left, original.top, original.right, original.bottom)
        }
    }
}

private fun LifecycleOwner.observeFolds(activity: Activity, view: View, onChanged: (List<FoldingFeature>, Rect) -> Unit): Job =
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            var features = emptyList<FoldingFeature>()
            var lastContainer: Rect? = null
            var lastFeatures: List<FoldingFeature>? = null
            fun update() {
                val activityLocation = IntArray(2)
                val viewLocation = IntArray(2)
                activity.window.decorView.getLocationOnScreen(activityLocation)
                view.getLocationOnScreen(viewLocation)
                val left = viewLocation[0] - activityLocation[0]
                val top = viewLocation[1] - activityLocation[1]
                val container = Rect(left, top, left + view.width, top + view.height)
                if (container != lastContainer || features != lastFeatures) {
                    lastContainer = container
                    lastFeatures = features
                    onChanged(features, container)
                }
            }
            val listener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> update() }
            view.addOnLayoutChangeListener(listener)
            try {
                WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { info ->
                    features = info.displayFeatures.filterIsInstance<FoldingFeature>()
                    if (features.isNotEmpty()) (activity as? one.mixin.android.ui.common.BaseActivity)?.onFoldObserved()
                    update()
                }
            } finally {
                view.removeOnLayoutChangeListener(listener)
            }
        }
    }
