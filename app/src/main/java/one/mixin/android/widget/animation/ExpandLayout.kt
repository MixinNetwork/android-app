package one.mixin.android.widget.animation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.Keep
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.extension.ANIMATION_DURATION_SHORT

class ExpandLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(
    context, attrs, defStyle
) {
    private val expandPath: Path = Path()
    private var mRunning = false
    private var p = 0f

    @Keep
    fun getP(): Float {
        return p
    }

    @Keep
    fun setP(p: Float) {
        this.p = p
        invalidate()
    }

    private val listener by lazy {
        object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator?) {
                mRunning = true
            }

            override fun onAnimationEnd(animation: Animator?) {
                mRunning = false
            }

            override fun onAnimationCancel(animation: Animator?) {
                mRunning = false
            }

            override fun onAnimationRepeat(animation: Animator?) {
                mRunning = true
            }
        }
    }

    fun collapse() {
        if (!mRunning) {
            ObjectAnimator.ofFloat(this, "p", 1f, 0f)
                .setDuration(ANIMATION_DURATION_SHORT)
                .apply {
                    interpolator = DecelerateInterpolator()
                    addListener(listener)
                }.start()
        }
    }

    fun expand() {
        if (!mRunning) {
            ObjectAnimator.ofFloat(this, "p", 0f, 1f)
                .setDuration(ANIMATION_DURATION_SHORT).apply {
                    interpolator = DecelerateInterpolator()
                    addListener(listener)
                }.start()
        }
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        if (mRunning) {
            val state = canvas.save()
            expandPath.reset()
            expandPath.addRect(
                RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight * p),
                Path.Direction.CW
            )
            canvas.clipPath(expandPath)
            val isInvalided = super.drawChild(canvas, child, drawingTime)
            canvas.restoreToCount(state)
            return isInvalided
        }
        return super.drawChild(canvas, child, drawingTime)
    }
}
