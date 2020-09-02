package one.mixin.android.widget.audio

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.view_slide_panel.view.*
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.formatMillis
import one.mixin.android.widget.AndroidUtilities
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.dip
import kotlin.math.abs

class SlidePanelView : RelativeLayout {

    private var blinkingDrawable: BlinkingDrawable? = null
    private var timeValue = 0
    private var toCanceled = false
    private var onEnding = false

    var callback: Callback? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_slide_panel, this, true)
        backgroundColor = context.colorFromAttribute(R.attr.bg_white)
        isClickable = true

        val blinkSize = context.resources.getDimensionPixelSize(R.dimen.blink_size)
        blinkingDrawable = BlinkingDrawable(ContextCompat.getColor(context, R.color.color_blink)).apply {
            setBounds(0, 0, blinkSize, blinkSize)
        }
        time_tv.setCompoundDrawables(blinkingDrawable, null, null, null)
        cancel_tv.setOnClickListener { callback?.onCancel() }
        time_tv.text = 0L.formatMillis()
    }

    fun onStart() {
        visibility = VISIBLE
        translationX = measuredWidth.toFloat()
        val animSet = AnimatorSet().apply {
            interpolator = DecelerateInterpolator()
            duration = 200
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        blinkingDrawable?.blinking()
                        postDelayed(updateTimeRunnable, 200)
                    }
                }
            )
        }
        animSet.playTogether(
            ObjectAnimator.ofFloat(this, "translationX", 0f),
            ObjectAnimator.ofFloat(this, "alpha", 1f)
        )
        animSet.start()
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
            slide_ll.translationX -= x * 1.5f
        }
        val alpha = abs(slide_ll.translationX * 1.5f / slide_ll.width)
        slide_ll.alpha = 1 - alpha
    }

    fun toCancel() {
        if (onEnding) return

        val animSet = AnimatorSet().apply {
            duration = 200
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
        onEnding = true
        val animSet = AnimatorSet().apply {
            interpolator = AccelerateInterpolator()
            duration = 200
            addListener(
                object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        handleEnd()
                        onEnding = false
                    }

                    override fun onAnimationCancel(animation: Animator?) {
                        handleEnd()
                        onEnding = false
                    }
                }
            )
        }
        animSet.playTogether(
            ObjectAnimator.ofFloat(this, "translationX", measuredWidth.toFloat()),
            ObjectAnimator.ofFloat(this, "alpha", 0f)
        )
        animSet.start()
    }

    private fun handleEnd() {
        toCanceled = false
        cancel_tv.alpha = 0f
        cancel_tv.translationY = -AndroidUtilities.dp(20f).toFloat()
        slide_ll.alpha = 1f
        slide_ll.translationY = 0f
        slide_ll.translationX = 0f

        blinkingDrawable?.stopBlinking()
        removeCallbacks(updateTimeRunnable)
        timeValue = 0
        time_tv.text = 0L.formatMillis()
    }

    private val updateTimeRunnable: Runnable by lazy {
        Runnable {
            if (timeValue > 59) {
                callback?.onTimeout()
                return@Runnable
            }

            timeValue++
            time_tv.text = (timeValue * 1000L).formatMillis()
            postDelayed(updateTimeRunnable, 1000)
        }
    }

    interface Callback {
        fun onTimeout()
        fun onCancel()
    }
}
