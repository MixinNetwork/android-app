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
import one.mixin.android.R
import one.mixin.android.databinding.ViewSlidePanelBinding
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import one.mixin.android.extension.formatMillis
import one.mixin.android.widget.AndroidUtilities
import org.jetbrains.anko.backgroundColor
import kotlin.math.abs

class SlidePanelView : RelativeLayout {

    private var blinkingDrawable: BlinkingDrawable? = null
    private var timeValue = 0
    private var toCanceled = false
    private var onEnding = false

    private val binding = ViewSlidePanelBinding.inflate(LayoutInflater.from(context), this)

    var callback: Callback? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        backgroundColor = context.colorFromAttribute(R.attr.bg_white)
        isClickable = true

        val blinkSize = context.resources.getDimensionPixelSize(R.dimen.blink_size)
        blinkingDrawable = BlinkingDrawable(ContextCompat.getColor(context, R.color.color_blink)).apply {
            setBounds(0, 0, blinkSize, blinkSize)
        }
        binding.timeTv.setCompoundDrawables(blinkingDrawable, null, null, null)
        binding.cancelTv.setOnClickListener { callback?.onCancel() }
        binding.timeTv.text = 0L.formatMillis()
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
        binding.slideLl.getLocationOnScreen(location)
        location[0] - 64.dp
    }

    fun slideText(x: Float) {
        val preX = binding.slideLl.translationX
        if (preX - x > 0) {
            binding.slideLl.translationX = 0f
        } else {
            binding.slideLl.translationX -= x * 1.5f
        }
        val alpha = abs(binding.slideLl.translationX * 1.5f / binding.slideLl.width)
        binding.slideLl.alpha = 1 - alpha
    }

    fun toCancel() {
        if (onEnding) return

        val animSet = AnimatorSet().apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }
        animSet.playTogether(
            ObjectAnimator.ofFloat(binding.slideLl, "alpha", 0f),
            ObjectAnimator.ofFloat(binding.slideLl, "translationY", AndroidUtilities.dp(20f).toFloat()),
            ObjectAnimator.ofFloat(binding.cancelTv, "alpha", 1f),
            ObjectAnimator.ofFloat(binding.cancelTv, "translationY", -AndroidUtilities.dp(20f).toFloat(), 0f)
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
        binding.cancelTv.alpha = 0f
        binding.cancelTv.translationY = -AndroidUtilities.dp(20f).toFloat()
        binding.slideLl.alpha = 1f
        binding.slideLl.translationY = 0f
        binding.slideLl.translationX = 0f

        blinkingDrawable?.stopBlinking()
        removeCallbacks(updateTimeRunnable)
        timeValue = 0
        binding.timeTv.text = 0L.formatMillis()
    }

    private val updateTimeRunnable: Runnable by lazy {
        Runnable {
            if (timeValue > 59) {
                callback?.onTimeout()
                return@Runnable
            }

            timeValue++
            binding.timeTv.text = (timeValue * 1000L).formatMillis()
            postDelayed(updateTimeRunnable, 1000)
        }
    }

    interface Callback {
        fun onTimeout()
        fun onCancel()
    }
}
