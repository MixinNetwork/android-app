package one.mixin.android.widget

import android.content.Context
import android.graphics.PointF
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import one.mixin.android.widget.AndroidUtilities.dp
import timber.log.Timber

class LinearSmoothScroller : SmoothScroller {
    protected val mLinearInterpolator = LinearInterpolator()
    protected val mDecelerateInterpolator = DecelerateInterpolator(1.5f)
    protected var mTargetVector: PointF? = null
    private val MILLISECONDS_PER_PX: Float
    protected var mInterimTargetDx = 0
    protected var mInterimTargetDy = 0
    private var scrollPosition: Int
    private var durationMultiplier = 1f
    private var offset = 0

    var fast = false

    constructor(context: Context, position: Int) {
        MILLISECONDS_PER_PX = MILLISECONDS_PER_INCH / context.resources.displayMetrics.densityDpi
        scrollPosition = position
    }

    constructor(context: Context, position: Int, durationMultiplier: Float) {
        this.durationMultiplier = durationMultiplier
        MILLISECONDS_PER_PX =
            MILLISECONDS_PER_INCH / context.resources.displayMetrics.densityDpi * durationMultiplier
        scrollPosition = position
    }

    override fun onStart() {}
    fun setOffset(offset: Int) {
        this.offset = offset
    }

    override fun onTargetFound(targetView: View, state: RecyclerView.State, action: Action) {
        val dy = calculateDyToMakeVisible(targetView)
        val time = calculateTimeForDeceleration(dy)
        Timber.e("onTargetFound $targetView $state $action $dy $time")
        if (time > 0) {
            action.update(
                0,
                -dy,
                if (fast) {
                    10
                } else {
                    300
                },
                mDecelerateInterpolator
            )
        } else {
            onEnd()
        }
    }

    override fun onSeekTargetStep(dx: Int, dy: Int, state: RecyclerView.State, action: Action) {
        Timber.e("onSeekTargetStep $dx $dy $state $action")
        if (childCount == 0) {
            stop()
            return
        }
        mInterimTargetDx = clampApplyScroll(mInterimTargetDx, dx)
        mInterimTargetDy = clampApplyScroll(mInterimTargetDy, dy)
        if (mInterimTargetDx == 0 && mInterimTargetDy == 0) {
            updateActionForInterimTarget(action)
        }
    }

    override fun onStop() {
        Timber.e("onStop")
        mInterimTargetDy = 0
        mInterimTargetDx = mInterimTargetDy
        mTargetVector = null
    }

    protected fun calculateTimeForDeceleration(dx: Int): Int {
        Timber.e("calculateTimeForDeceleration $dx")
        return Math.ceil(calculateTimeForScrolling(dx) / .3356).toInt()
    }

    protected fun calculateTimeForScrolling(dx: Int): Int {
        Timber.e("calculateTimeForScrolling $dx")
        return Math.ceil((Math.abs(dx) * MILLISECONDS_PER_PX).toDouble()).toInt()
    }

    protected fun updateActionForInterimTarget(action: Action) {
        val scrollVector = computeScrollVectorForPosition(targetPosition)
        if (scrollVector == null || scrollVector.x == 0f && scrollVector.y == 0f) {
            val target = targetPosition
            action.jumpTo(target)
            stop()
            return
        }
        normalize(scrollVector)
        mTargetVector = scrollVector
        mInterimTargetDx = (TARGET_SEEK_SCROLL_DISTANCE_PX * scrollVector.x).toInt()
        mInterimTargetDy = (TARGET_SEEK_SCROLL_DISTANCE_PX * scrollVector.y).toInt()
        val time = calculateTimeForScrolling(TARGET_SEEK_SCROLL_DISTANCE_PX)
        action.update(
            (mInterimTargetDx * TARGET_SEEK_EXTRA_SCROLL_RATIO).toInt(),
            (mInterimTargetDy * TARGET_SEEK_EXTRA_SCROLL_RATIO).toInt(),
            (time * TARGET_SEEK_EXTRA_SCROLL_RATIO).toInt(),
            mLinearInterpolator
        )
    }

    private fun clampApplyScroll(tmpDt: Int, dt: Int): Int {
        var tmpDt = tmpDt
        val before = tmpDt
        tmpDt -= dt
        return if (before * tmpDt <= 0) {
            0
        } else tmpDt
    }

    fun calculateDyToMakeVisible(view: View): Int {
        val layoutManager = layoutManager
        if (layoutManager == null || !layoutManager.canScrollVertically()) {
            return 0
        }
        val params = view.layoutParams as RecyclerView.LayoutParams
        val top = layoutManager.getDecoratedTop(view) - params.topMargin
        val bottom = layoutManager.getDecoratedBottom(view) + params.bottomMargin
        var start = layoutManager.paddingTop
        var end = layoutManager.height - layoutManager.paddingBottom
        val boxSize = end - start
        val viewSize = bottom - top
        start =
            if (scrollPosition == POSITION_TOP) {
                layoutManager.paddingTop + offset
            } else if (viewSize > boxSize) {
                0
            } else if (scrollPosition == POSITION_MIDDLE) {
                (boxSize - viewSize) / 2
            } else {
                layoutManager.paddingTop + offset - dp(88f)
            }
        end = start + viewSize
        val dtStart = start - top
        if (dtStart > 0) {
            return dtStart
        }
        val dtEnd = end - bottom
        return if (dtEnd < 0) {
            dtEnd
        } else 0
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        val layoutManager = layoutManager
        Timber.e("computeScrollVectorForPosition $targetPosition")
        return if (layoutManager is ScrollVectorProvider) {
            (layoutManager as ScrollVectorProvider).computeScrollVectorForPosition(targetPosition)
        } else null
    }

    fun onEnd() {}

    companion object {
        private const val MILLISECONDS_PER_INCH = 65f
        private const val TARGET_SEEK_SCROLL_DISTANCE_PX = 10000
        private const val TARGET_SEEK_EXTRA_SCROLL_RATIO = 1.2f
        const val POSITION_MIDDLE = 0
        const val POSITION_END = 1
        const val POSITION_TOP = 2
    }
}
