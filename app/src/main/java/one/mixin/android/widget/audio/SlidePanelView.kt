package one.mixin.android.widget.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_slide_panel.view.*
import one.mixin.android.R
import one.mixin.android.extension.vibrate

class SlidePanelView: FrameLayout {

    private var blinkingDrawable: BlinkingDrawable? = null
    private var timeValue = 0

    var callback: Callback? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_slide_panel, this, true)

        val blinkSize = context.resources.getDimensionPixelSize(R.dimen.blink_size)
        blinkingDrawable = BlinkingDrawable(ContextCompat.getColor(context, R.color.color_blink)).apply {
            setBounds(0, 0, blinkSize, blinkSize)
        }
        time_tv.setCompoundDrawables(blinkingDrawable, null, null, null)
    }

    fun onStart() {
        context.vibrate(longArrayOf(0, 30))
        visibility = VISIBLE
        translationX = (width).toFloat()
        animate().apply {
            translationX(0f)
            alpha(1f)
            interpolator = DecelerateInterpolator()
            duration = 200
            setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    setListener(null)

                    blinkingDrawable?.blinking()
                    post(updateTimeRunnable)
                }
            })
        }.start()
    }

    fun slideText(x: Float) {
        val preX = slide_ll.scrollX
        if (preX + x < 0) {
            slide_ll.scrollBy(0, 0)
        } else {
            slide_ll.scrollBy(x.toInt(), 0)
        }
        val alpha = slide_ll.scrollX.toFloat() / slide_ll.width
        slide_ll.alpha = 1 - alpha
    }

    fun onEnd() {
        context.vibrate(longArrayOf(0, 30))
        animate().apply {
            translationX(width.toFloat())
            alpha(0f)
            interpolator = AccelerateInterpolator()
            duration = 200
            setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    handleEnd()
                }

                override fun onAnimationCancel(animation: Animator?) {
                    handleEnd()
                }
            })
        }.start()
    }

    private fun handleEnd() {
        animate().setListener(null)
        slide_ll.scrollX = 0
        slide_ll.alpha = 1f

        blinkingDrawable?.stopBlinking()
        removeCallbacks(updateTimeRunnable)
        timeValue = 0
        time_tv.text = context.getString(R.string.time, 0)
    }

    private val updateTimeRunnable: Runnable by lazy {
        Runnable {
            if (timeValue > 59) {
                callback?.onTimeout()
                return@Runnable
            }

            timeValue++
            time_tv.text = context.getString(R.string.time, timeValue)
            postDelayed(updateTimeRunnable, 1000)
        }
    }

    interface Callback {
        fun onTimeout()
    }
}