package one.mixin.android.ui.media.pager.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import one.mixin.android.ui.media.pager.PlayerView
import one.mixin.android.widget.PhotoView.PhotoView
import one.mixin.android.widget.largeimage.LargeImageView
import kotlin.math.abs

/**
 * Hardware-property driven vertical drag-to-dismiss container (zero requestLayout during drag).
 * Coordinates smoothly with ViewPager2 horizontal paging, PhotoView pinch-zoom,
 * LargeImageView vertical scrolling, and PlayerView scrubbing.
 */
@OptIn(UnstableApi::class)
class MediaDismissLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    interface OnDismissListener {
        fun onDragStart() {}

        fun onDragProgress(progress: Float)

        fun onDismiss(velocityY: Float)

        fun onDragCancel()
    }

    var dismissListener: OnDismissListener? = null
    var dismissEnabled: Boolean = true

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity * 3.5f

    private var downX = 0f
    private var downY = 0f
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var isDragging = false
    private var ignoreGesture = false
    private var velocityTracker: VelocityTracker? = null
    private var settleAnimator: ValueAnimator? = null

    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var dragScale = 1f
    private var dragProgress = 0f

    val currentTranslationX: Float get() = dragOffsetX
    val currentTranslationY: Float get() = dragOffsetY
    val currentScale: Float get() = dragScale

    fun resetTransformImmediate() {
        settleAnimator?.cancel()
        settleAnimator = null
        isDragging = false
        dragOffsetX = 0f
        dragOffsetY = 0f
        dragScale = 1f
        dragProgress = 0f
        applyTransformToTarget()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!dismissEnabled) return false

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                settleAnimator?.cancel()
                downX = ev.rawX
                downY = ev.rawY
                isDragging = false
                ignoreGesture = false
                recycleTracker()
                velocityTracker = VelocityTracker.obtain().apply { addMovement(ev) }
                return false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                ignoreGesture = true
                if (isDragging) {
                    finishDrag(canceled = true)
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (ignoreGesture || ev.pointerCount > 1) return false
                velocityTracker?.addMovement(ev)
                val dx = ev.rawX - downX
                val dy = ev.rawY - downY
                val absDx = abs(dx)
                val absDy = abs(dy)

                if (absDx > touchSlop && absDx > absDy) {
                    // Horizontal swipe belongs to ViewPager2 or zoomed image panning
                    ignoreGesture = true
                    return false
                }

                if (absDy > touchSlop && absDy > absDx * 1.15f) {
                    if (canStartVerticalDismiss(dy)) {
                        isDragging = true
                        dragStartX = ev.rawX
                        dragStartY = ev.rawY
                        parent?.requestDisallowInterceptTouchEvent(true)
                        dismissListener?.onDragStart()
                        return true
                    } else {
                        ignoreGesture = true
                    }
                }
                return false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                ignoreGesture = false
                recycleTracker()
                return false
            }
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!dismissEnabled) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.rawX
                downY = event.rawY
                recycleTracker()
                velocityTracker = VelocityTracker.obtain().apply { addMovement(event) }
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (isDragging) {
                    finishDrag(canceled = true)
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) return false
                velocityTracker?.addMovement(event)
                val dx = event.rawX - dragStartX
                val dy = event.rawY - dragStartY
                updateDragTransform(dx, dy)
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging) {
                    velocityTracker?.addMovement(event)
                    finishDrag(canceled = false)
                    return true
                }
                recycleTracker()
                return false
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    finishDrag(canceled = true)
                    return true
                }
                recycleTracker()
                return false
            }
        }
        return super.onTouchEvent(event)
    }

    private fun updateDragTransform(dx: Float, dy: Float) {
        val h = height.coerceAtLeast(1).toFloat()
        val progress = (abs(dy) / (h * 0.48f)).coerceIn(0f, 1f)
        dragProgress = progress
        dragOffsetX = dx * 0.85f
        dragOffsetY = dy
        dragScale = (1f - abs(dy) / (h * 1.45f)).coerceIn(MIN_DRAG_SCALE, 1f)
        applyTransformToTarget()
        dismissListener?.onDragProgress(progress)
    }

    private fun finishDrag(canceled: Boolean) {
        isDragging = false
        parent?.requestDisallowInterceptTouchEvent(false)
        val vy =
            velocityTracker?.let { tracker ->
                tracker.computeCurrentVelocity(1000)
                tracker.yVelocity
            } ?: 0f
        recycleTracker()

        val h = height.coerceAtLeast(1).toFloat()
        val shouldDismiss =
            !canceled && (
                abs(dragOffsetY) > h * DISMISS_DISTANCE_FRACTION ||
                    (abs(vy) > minFlingVelocity && abs(dragOffsetY) > touchSlop * 2f)
                )

        if (shouldDismiss) {
            dismissListener?.onDismiss(vy)
        } else {
            animateSnapBack()
        }
    }

    private fun animateSnapBack() {
        val startX = dragOffsetX
        val startY = dragOffsetY
        val startScale = dragScale
        val startProgress = dragProgress

        settleAnimator?.cancel()
        settleAnimator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = SNAP_BACK_DURATION_MS
                interpolator = SNAP_BACK_INTERPOLATOR
                addUpdateListener { anim ->
                    val t = anim.animatedValue as Float
                    dragOffsetX = startX * (1f - t)
                    dragOffsetY = startY * (1f - t)
                    dragScale = startScale + (1f - startScale) * t
                    dragProgress = startProgress * (1f - t)
                    applyTransformToTarget()
                    dismissListener?.onDragProgress(dragProgress)
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            settleAnimator = null
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                            dragScale = 1f
                            dragProgress = 0f
                            applyTransformToTarget()
                            dismissListener?.onDragCancel()
                        }
                    },
                )
                start()
            }
    }

    private fun applyTransformToTarget() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.translationX = dragOffsetX
            child.translationY = dragOffsetY
            child.scaleX = dragScale
            child.scaleY = dragScale
        }
    }

    private fun canStartVerticalDismiss(dy: Float): Boolean {
        val child = getPrimaryContentChild() ?: return true
        return when (child) {
            is PhotoView -> child.scale <= 1.02f
            is LargeImageView -> {
                if (child.scale > child.minScale + 0.05f && child.scale > 1.02f) {
                    false
                } else if (dy > 0f) {
                    child.scrollY <= 0
                } else {
                    !child.canScrollVertically(1)
                }
            }
            is PlayerView -> !child.playerControlView.scrubbing
            else -> true
        }
    }

    fun getPrimaryContentChild(): View? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is PhotoView || child is LargeImageView || child is PlayerView) {
                if (child.visibility != GONE) return child
            }
        }
        return if (childCount > 0) getChildAt(0) else null
    }

    private fun recycleTracker() {
        velocityTracker?.recycle()
        velocityTracker = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        settleAnimator?.cancel()
        settleAnimator = null
        recycleTracker()
    }

    companion object {
        private const val MIN_DRAG_SCALE = 0.62f
        private const val DISMISS_DISTANCE_FRACTION = 0.13f
        private const val SNAP_BACK_DURATION_MS = 220L
        private val SNAP_BACK_INTERPOLATOR = PathInterpolator(0.2f, 0.88f, 0.3f, 1f)
    }
}
