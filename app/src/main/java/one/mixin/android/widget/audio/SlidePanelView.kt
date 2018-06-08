package one.mixin.android.widget.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.view_slide_panel.view.*
import one.mixin.android.R
import one.mixin.android.extension.vibrate
import one.mixin.android.widget.AndroidUtilities
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.dip
import kotlin.math.abs

class SlidePanelView : RelativeLayout {

    private var blinkingDrawable: BlinkingDrawable? = null
    private var timeValue = 0
    private var toCanceled = false

    var callback: Callback? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_slide_panel, this, true)
        backgroundColor = Color.WHITE
        isClickable = true

        val blinkSize = context.resources.getDimensionPixelSize(R.dimen.blink_size)
        blinkingDrawable = BlinkingDrawable(ContextCompat.getColor(context, R.color.color_blink)).apply {
            setBounds(0, 0, blinkSize, blinkSize)
        }
        time_tv.setCompoundDrawables(blinkingDrawable, null, null, null)
        cancel_tv.setOnClickListener { callback?.onCancel() }
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

    val slideWidth by lazy {
        val location = IntArray(2)
        slide_ll.getLocationOnScreen(location)
        location[0] - context.dip(64)
    }

    fun slideText(x: Float) {
        val preX = slide_ll.translationX
        if (preX - x > 0) {
            slide_ll.translationX = 0f
        } else {
            slide_ll.translationX -= x
        }
        val alpha = abs(slide_ll.translationX / slide_ll.width)
        slide_ll.alpha = 1 - alpha
    }

    fun toCancel() {
        val animSet = AnimatorSet().apply {
            duration = 150
            interpolator = DecelerateInterpolator()
        }
        animSet.playTogether(
            ObjectAnimator.ofFloat(slide_ll, "alpha", 0f),
            ObjectAnimator.ofFloat(slide_ll, "translationY", AndroidUtilities.dp(20f).toFloat()),
            ObjectAnimator.ofFloat(cancel_tv, "alpha", 1f),
            ObjectAnimator.ofFloat(cancel_tv, "translationY", -AndroidUtilities.dp(20f).toFloat(), 0f)
        )
        animSet.start()
        toCanceled = true
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
        if (toCanceled) {
            toCanceled = false
            cancel_tv.alpha = 0f
            cancel_tv.translationY = 0f
            slide_ll.alpha = 1f
            slide_ll.translationY = 0f
        }
        slide_ll.translationX = 0f

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
        fun onCancel()
    }
}