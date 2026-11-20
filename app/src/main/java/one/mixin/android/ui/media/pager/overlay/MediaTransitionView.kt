package one.mixin.android.ui.media.pager.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.animation.PathInterpolator
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Fullscreen canvas layer that renders Telegram X-style geometry morph transitions
 * (rect + CENTER_CROP uncropping + half-occluded clipRect + 8-value corner radii).
 */
class MediaTransitionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private var drawable: Drawable? = null
    private var animator: ValueAnimator? = null

    private val startImageRect = RectF()
    private val endImageRect = RectF()
    private val currentImageRect = RectF()

    private val startClipRect = RectF()
    private val endClipRect = RectF()
    private val currentClipRect = RectF()

    private val startRadii = FloatArray(8)
    private val endRadii = FloatArray(8)
    private val currentRadii = FloatArray(8)

    private var startAlpha = 1f
    private var endAlpha = 1f
    private var currentAlpha = 1f

    private val clipPath = Path()
    private val screenOffset = IntArray(2)

    val isAnimating: Boolean
        get() = animator?.isRunning == true

    fun cancelAnimation() {
        animator?.cancel()
        animator = null
    }

    fun clearAndHide() {
        cancelAnimation()
        visibility = INVISIBLE
        drawable = null
    }

    fun animateOpen(
        location: MediaThumbLocation,
        onProgress: (Float) -> Unit,
        onEnd: () -> Unit,
    ) {
        cancelAnimation()
        drawable = location.drawable
        if (drawable == null || width <= 0 || height <= 0) {
            onProgress(1f)
            onEnd()
            return
        }

        getLocationOnScreen(screenOffset)
        val localThumbRect = toLocalRect(location.screenRect)
        val localClipRect = toLocalRect(location.clipRect)

        val contentW = resolveContentWidth(location.contentWidth, drawable!!)
        val contentH = resolveContentHeight(location.contentHeight, drawable!!)

        computeSourceImageRect(localThumbRect, contentW, contentH, location.scaleType, startImageRect)
        computeFitCenterRect(width.toFloat(), height.toFloat(), contentW, contentH, endImageRect)

        startClipRect.set(localClipRect)
        endClipRect.set(endImageRect)

        location.cornerRadii.copyInto(startRadii)
        endRadii.fill(0f)

        startAlpha = 1f
        endAlpha = 1f
        currentAlpha = 1f

        applyProgress(0f)
        visibility = VISIBLE

        startValueAnimator(
            durationMs = OPEN_DURATION_MS,
            interpolator = OPEN_INTERPOLATOR,
            onUpdate = { fraction ->
                applyProgress(fraction)
                onProgress(fraction)
            },
            onComplete = {
                onEnd()
            },
        )
    }

    fun animateCloseToTarget(
        currentScreenRect: RectF,
        currentDrawable: Drawable?,
        targetLocation: MediaThumbLocation,
        velocityY: Float = 0f,
        onProgress: (Float) -> Unit,
        onEnd: () -> Unit,
    ) {
        cancelAnimation()
        val transitionDrawable = currentDrawable ?: targetLocation.drawable
        drawable = transitionDrawable
        if (transitionDrawable == null || width <= 0 || height <= 0) {
            onProgress(1f)
            onEnd()
            return
        }

        getLocationOnScreen(screenOffset)
        val localCurrentRect = toLocalRect(currentScreenRect)
        val localTargetThumbRect = toLocalRect(targetLocation.screenRect)
        val localTargetClipRect = toLocalRect(targetLocation.clipRect)

        val contentW = resolveContentWidth(targetLocation.contentWidth, transitionDrawable)
        val contentH = resolveContentHeight(targetLocation.contentHeight, transitionDrawable)

        startImageRect.set(localCurrentRect)
        startClipRect.set(localCurrentRect)
        computeSourceImageRect(
            localTargetThumbRect,
            contentW,
            contentH,
            targetLocation.scaleType,
            endImageRect,
        )
        endClipRect.set(localTargetClipRect)

        startRadii.fill(0f)
        targetLocation.cornerRadii.copyInto(endRadii)

        startAlpha = 1f
        endAlpha = 1f
        currentAlpha = 1f

        applyProgress(0f)
        visibility = VISIBLE

        val speedFactor = (abs(velocityY) / 4500f).coerceIn(0f, 1f)
        val durationMs = (CLOSE_DURATION_MAX_MS - speedFactor * (CLOSE_DURATION_MAX_MS - CLOSE_DURATION_MIN_MS)).toLong()

        startValueAnimator(
            durationMs = durationMs,
            interpolator = CLOSE_INTERPOLATOR,
            onUpdate = { fraction ->
                applyProgress(fraction)
                onProgress(fraction)
            },
            onComplete = {
                onEnd()
            },
        )
    }

    fun animateFadeClose(
        currentScreenRect: RectF,
        currentDrawable: Drawable?,
        velocityY: Float = 0f,
        onProgress: (Float) -> Unit,
        onEnd: () -> Unit,
    ) {
        cancelAnimation()
        drawable = currentDrawable
        if (currentDrawable == null || width <= 0 || height <= 0) {
            onProgress(1f)
            onEnd()
            return
        }

        getLocationOnScreen(screenOffset)
        val localCurrentRect = toLocalRect(currentScreenRect)
        startImageRect.set(localCurrentRect)
        startClipRect.set(localCurrentRect)

        val targetScale = 0.84f
        val extraY =
            when {
                velocityY > 400f -> height * 0.14f
                velocityY < -400f -> -height * 0.14f
                else -> 0f
            }
        val cx = localCurrentRect.centerX()
        val cy = localCurrentRect.centerY() + extraY
        val hw = localCurrentRect.width() * targetScale / 2f
        val hh = localCurrentRect.height() * targetScale / 2f
        endImageRect.set(cx - hw, cy - hh, cx + hw, cy + hh)
        endClipRect.set(endImageRect)

        startRadii.fill(0f)
        endRadii.fill(0f)

        startAlpha = 1f
        endAlpha = 0f
        currentAlpha = 1f

        applyProgress(0f)
        visibility = VISIBLE

        startValueAnimator(
            durationMs = FADE_CLOSE_DURATION_MS,
            interpolator = CLOSE_INTERPOLATOR,
            onUpdate = { fraction ->
                applyProgress(fraction)
                onProgress(fraction)
            },
            onComplete = {
                onEnd()
            },
        )
    }

    private fun startValueAnimator(
        durationMs: Long,
        interpolator: PathInterpolator,
        onUpdate: (Float) -> Unit,
        onComplete: () -> Unit,
    ) {
        var canceled = false
        animator =
            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = durationMs
                this.interpolator = interpolator
                addUpdateListener { anim ->
                    onUpdate(anim.animatedValue as Float)
                }
                addListener(
                    object : AnimatorListenerAdapter() {
                        override fun onAnimationCancel(animation: Animator) {
                            canceled = true
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            animator = null
                            if (!canceled) {
                                onComplete()
                            }
                        }
                    },
                )
                start()
            }
    }

    private fun applyProgress(t: Float) {
        lerpRect(startImageRect, endImageRect, t, currentImageRect)
        lerpRect(startClipRect, endClipRect, t, currentClipRect)
        for (i in 0 until 8) {
            currentRadii[i] = startRadii[i] + (endRadii[i] - startRadii[i]) * t
        }
        currentAlpha = startAlpha + (endAlpha - startAlpha) * t
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val d = drawable ?: return
        if (currentImageRect.width() <= 0f || currentImageRect.height() <= 0f) return

        val saveCount = canvas.save()
        clipPath.reset()
        clipPath.addRoundRect(currentClipRect, currentRadii, Path.Direction.CW)
        canvas.clipPath(clipPath)

        val prevBounds = d.copyBounds()
        d.alpha = (currentAlpha.coerceIn(0f, 1f) * 255f).roundToInt()
        d.setBounds(
            currentImageRect.left.roundToInt(),
            currentImageRect.top.roundToInt(),
            currentImageRect.right.roundToInt(),
            currentImageRect.bottom.roundToInt(),
        )
        d.draw(canvas)
        d.bounds = prevBounds
        d.alpha = 255
        canvas.restoreToCount(saveCount)
    }

    private fun toLocalRect(screenRect: RectF): RectF =
        RectF(
            screenRect.left - screenOffset[0],
            screenRect.top - screenOffset[1],
            screenRect.right - screenOffset[0],
            screenRect.bottom - screenOffset[1],
        )

    companion object {
        private const val OPEN_DURATION_MS = 240L
        private const val CLOSE_DURATION_MAX_MS = 235L
        private const val CLOSE_DURATION_MIN_MS = 165L
        private const val FADE_CLOSE_DURATION_MS = 190L

        private val OPEN_INTERPOLATOR = PathInterpolator(0.2f, 0.88f, 0.3f, 1f)
        private val CLOSE_INTERPOLATOR = PathInterpolator(0.22f, 0.86f, 0.32f, 1f)

        fun computeFitCenterRect(
            containerW: Float,
            containerH: Float,
            contentW: Int,
            contentH: Int,
            out: RectF,
        ) {
            if (containerW <= 0f || containerH <= 0f || contentW <= 0 || contentH <= 0) {
                out.set(0f, 0f, containerW, containerH)
                return
            }
            val containerRatio = containerW / containerH
            val contentRatio = contentW.toFloat() / contentH.toFloat()
            val targetW: Float
            val targetH: Float
            if (contentRatio > containerRatio) {
                targetW = containerW
                targetH = containerW / contentRatio
            } else {
                targetH = containerH
                targetW = containerH * contentRatio
            }
            val left = (containerW - targetW) / 2f
            val top = (containerH - targetH) / 2f
            out.set(left, top, left + targetW, top + targetH)
        }

        private fun computeSourceImageRect(
            thumbRect: RectF,
            contentW: Int,
            contentH: Int,
            scaleType: ImageView.ScaleType,
            out: RectF,
        ) {
            if (scaleType != ImageView.ScaleType.CENTER_CROP || contentW <= 0 || contentH <= 0) {
                out.set(thumbRect)
                return
            }
            val thumbW = thumbRect.width()
            val thumbH = thumbRect.height()
            if (thumbW <= 0f || thumbH <= 0f) {
                out.set(thumbRect)
                return
            }
            val thumbRatio = thumbW / thumbH
            val contentRatio = contentW.toFloat() / contentH.toFloat()
            val drawW: Float
            val drawH: Float
            if (contentRatio > thumbRatio) {
                drawH = thumbH
                drawW = thumbH * contentRatio
            } else {
                drawW = thumbW
                drawH = thumbW / contentRatio
            }
            val cx = thumbRect.centerX()
            val cy = thumbRect.centerY()
            out.set(cx - drawW / 2f, cy - drawH / 2f, cx + drawW / 2f, cy + drawH / 2f)
        }

        private fun resolveContentWidth(contentW: Int, drawable: Drawable): Int =
            when {
                contentW > 0 -> contentW
                drawable.intrinsicWidth > 0 -> drawable.intrinsicWidth
                else -> 1
            }

        private fun resolveContentHeight(contentH: Int, drawable: Drawable): Int =
            when {
                contentH > 0 -> contentH
                drawable.intrinsicHeight > 0 -> drawable.intrinsicHeight
                else -> 1
            }

        private fun lerpRect(start: RectF, end: RectF, t: Float, out: RectF) {
            out.set(
                start.left + (end.left - start.left) * t,
                start.top + (end.top - start.top) * t,
                start.right + (end.right - start.right) * t,
                start.bottom + (end.bottom - start.bottom) * t,
            )
        }
    }
}
