package one.mixin.android.widget.theme

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.RelativeLayout
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.NightModeButtonLayoutBinding
import one.mixin.android.extension.dp
import timber.log.Timber

class NightModeSwitch : RelativeLayout {
    companion object {
        const val SWITCH_DURATION = 200L
        const val ANIM_DURATION = 230L
    }

    constructor(context: Context) : super(context) {
        init()
    }

    // Listener
    private var onSwitchListener: ((Int) -> Unit)? = null
    private var inAnimation = false
    private val androidQ = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private val surplusWidth = 35f

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private var state = Constants.Theme.THEME_LIGHT_ID

    private val binding = NightModeButtonLayoutBinding.inflate(LayoutInflater.from(context), this)

    // Views
    private val switchIV = binding.switchIV
    private val switchRL = binding.switchRL

    @SuppressLint("UseCompatLoadingForDrawables")
    fun initState(state: Int) {
        Timber.e("initState $state")
        this.state = state
        when (state) {
            Constants.Theme.THEME_LIGHT_ID -> {
                switchIV.translationX = 0f
                switchIV.setImageDrawable(context.getDrawable(R.drawable.ic_sun))
                switchRL.setCardBackgroundColor(Color.parseColor("#F6F7FA"))
            }
            Constants.Theme.THEME_AUTO_ID -> {
                switchIV.translationX = (surplusWidth).dp.toFloat()
                switchIV.setImageDrawable(context.getDrawable(R.drawable.ic_sun_moom))
                switchRL.setCardBackgroundColor(Color.parseColor("#8d8f93"))
            }
            Constants.Theme.THEME_NIGHT_ID -> {
                switchIV.translationX = (surplusWidth.dp * (if (androidQ) 2f else 1f))
                switchIV.setImageDrawable(context.getDrawable(R.drawable.ic_moom))
                switchRL.setCardBackgroundColor(Color.parseColor("#23272B"))
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        Timber.e("init $androidQ")
        switchRL.layoutParams.width = if (androidQ) {
            (111).dp
        } else {
            75.dp
        }
        if (androidQ) {
            switchRL.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    if (event.x <= v.width / 3) {
                        if (state != Constants.Theme.THEME_LIGHT_ID) {
                            switch(Constants.Theme.THEME_LIGHT_ID)
                        }
                    } else if (event.x <= v.width / 3 * 2) {
                        if (state != Constants.Theme.THEME_AUTO_ID) {
                            switch(Constants.Theme.THEME_AUTO_ID)
                        }
                    } else {
                        if (state != Constants.Theme.THEME_NIGHT_ID) {
                            switch(Constants.Theme.THEME_NIGHT_ID)
                        }
                    }
                }
                true
            }
        } else {
            switchRL.setOnClickListener {
                switch(
                    if (state == Constants.Theme.THEME_NIGHT_ID) {
                        Constants.Theme.THEME_LIGHT_ID
                    } else {
                        Constants.Theme.THEME_NIGHT_ID
                    }
                )
            }
        }
    }

    fun switch() {
        if (androidQ) {
            switch(
                when (state) {
                    Constants.Theme.THEME_LIGHT_ID -> {
                        Constants.Theme.THEME_AUTO_ID
                    }
                    Constants.Theme.THEME_NIGHT_ID -> {
                        Constants.Theme.THEME_LIGHT_ID
                    }
                    else -> {
                        Constants.Theme.THEME_NIGHT_ID
                    }
                }
            )
        } else {
            switch(
                if (state == Constants.Theme.THEME_NIGHT_ID) {
                    Constants.Theme.THEME_LIGHT_ID
                } else {
                    Constants.Theme.THEME_NIGHT_ID
                }
            )
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun switch(state: Int) {
        if (inAnimation) return
        if (this.state == Constants.Theme.THEME_LIGHT_ID) {
            switchIV.setImageDrawable(context.getDrawable(if (state == Constants.Theme.THEME_NIGHT_ID) R.drawable.ic_moom else R.drawable.ic_sun_moom))
        }
        when (state) {
            Constants.Theme.THEME_NIGHT_ID -> {
                inAnimation = true
                ObjectAnimator
                    .ofFloat(switchIV, "rotation", 0f, 360f)
                    .setDuration(SWITCH_DURATION)
                    .start()
                ObjectAnimator
                    .ofFloat(switchIV, "translationX", switchIV.translationX, surplusWidth.dp * (if (androidQ) 2f else 1f))
                    .setDuration(SWITCH_DURATION)
                    .start()
                val valueAnimator = ValueAnimator.ofArgb(
                    getBgColor(this@NightModeSwitch.state),
                    getBgColor(state)
                )
                valueAnimator.duration = SWITCH_DURATION
                valueAnimator.addUpdateListener { animation ->
                    switchRL.setCardBackgroundColor(
                        animation.animatedValue as Int
                    )
                }
                valueAnimator.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animator: Animator) {}
                    override fun onAnimationEnd(animator: Animator) {
                        inAnimation = false
                        this@NightModeSwitch.state = state
                        switchIV.setImageDrawable(context.getDrawable(R.drawable.ic_moom))
                        nightModeButtonClicked(state)
                    }

                    override fun onAnimationCancel(animator: Animator) {}
                    override fun onAnimationRepeat(animator: Animator) {}
                })
                valueAnimator.start()
            }
            Constants.Theme.THEME_AUTO_ID -> {
                inAnimation = true
                ObjectAnimator
                    .ofFloat(switchIV, "rotation", 0f, 360f)
                    .setDuration(SWITCH_DURATION)
                    .start()
                ObjectAnimator
                    .ofFloat(switchIV, "translationX", switchIV.translationX, surplusWidth.dp.toFloat())
                    .setDuration(SWITCH_DURATION)
                    .start()
                val valueAnimator = ValueAnimator.ofArgb(
                    getBgColor(this@NightModeSwitch.state),
                    getBgColor(state)
                )
                valueAnimator.duration = SWITCH_DURATION
                valueAnimator.addUpdateListener { animation ->
                    switchRL.setCardBackgroundColor(
                        animation.animatedValue as Int
                    )
                }
                valueAnimator.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animator: Animator) {}
                    override fun onAnimationEnd(animator: Animator) {
                        inAnimation = false
                        this@NightModeSwitch.state = state
                        switchIV.setImageDrawable(context.getDrawable(R.drawable.ic_sun_moom))
                        nightModeButtonClicked(state)
                    }

                    override fun onAnimationCancel(animator: Animator) {}
                    override fun onAnimationRepeat(animator: Animator) {}
                })
                valueAnimator.start()
            }
            else -> {
                inAnimation = true
                ObjectAnimator
                    .ofFloat(switchIV, "rotation", 0f, 360f)
                    .setDuration(SWITCH_DURATION)
                    .start()
                ObjectAnimator
                    .ofFloat(switchIV, "translationX", switchIV.translationX, 0f)
                    .setDuration(SWITCH_DURATION)
                    .start()
                val valueAnimator = ValueAnimator.ofArgb(
                    getBgColor(this@NightModeSwitch.state),
                    getBgColor(state)
                )
                valueAnimator.duration = SWITCH_DURATION
                valueAnimator.addUpdateListener { animation ->
                    switchRL.setCardBackgroundColor(
                        animation.animatedValue as Int
                    )
                }
                valueAnimator.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animator: Animator) {}
                    override fun onAnimationEnd(animator: Animator) {
                        inAnimation = false
                        this@NightModeSwitch.state = state
                        switchIV.setImageDrawable(context.getDrawable(R.drawable.ic_sun))
                        nightModeButtonClicked(state)
                    }

                    override fun onAnimationCancel(animator: Animator) {}
                    override fun onAnimationRepeat(animator: Animator) {}
                })
                valueAnimator.start()
            }
        }
    }

    fun setOnSwitchListener(onSwitchListener: (Int) -> Unit) {
        this.onSwitchListener = onSwitchListener
    }

    private fun nightModeButtonClicked(state: Int) {
        onSwitchListener?.invoke(state)
    }

    private fun getBgColor(state: Int): Int {
        return when (state) {
            Constants.Theme.THEME_LIGHT_ID -> Color.parseColor("#F6F7FA")
            Constants.Theme.THEME_NIGHT_ID -> Color.parseColor("#23272B")
            else -> Color.parseColor("#8d8f93")
        }
    }
}
