package one.mixin.android.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.MeasureSpec.AT_MOST
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowInsets
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentDialog
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnPreDraw
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.displayMetrics
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.getSystemWindowBottom
import one.mixin.android.extension.getSystemWindowLeft
import one.mixin.android.extension.getSystemWindowRight
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.isNotchScreen
import one.mixin.android.extension.isTablet
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.realSize
import one.mixin.android.extension.statusBarHeight
import one.mixin.android.util.SystemUIManager
import one.mixin.android.widget.AndroidUtilities.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class BottomSheet(
    context: Context,
    private val focusable: Boolean,
    private val softInputResize: Boolean,
) : ComponentDialog(context, R.style.TransparentDialog) {
    private var startAnimationRunnable: Runnable? = null
    private var curSheetAnimation: AnimatorSet? = null
    private var isDismissed = false
    private var isShown = false
    var lastInsets: WindowInsets? = null

    private val container: ContainerView by lazy { ContainerView(context) }
    private val sheetContainer: FrameLayout by lazy { FrameLayout(context) }
    private var customView: View? = null
    private var customViewHeight: Int = 0

    private val speed = 0.5f.dp

    private val backDrawable = ColorDrawable(-0x1000000)

    private var bottomSheetListener: BottomSheetListener? = null

    var dismissClickOutside = true
        set(value) {
            field = value
            if (value) {
                setOnKeyListener(null)
            } else {
                setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        return@setOnKeyListener true
                    }
                    return@setOnKeyListener false
                }
            }
        }

    private inner class ContainerView(context: Context) : FrameLayout(context) {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(ev: MotionEvent?): Boolean {
            if (ev != null && (ev.action == MotionEvent.ACTION_DOWN || ev.action == MotionEvent.ACTION_MOVE)) {
                val startX = ev.x.toInt()
                val startY = ev.y.toInt()
                if (startY < sheetContainer.top || startX < sheetContainer.left || startX > sheetContainer.right) {
                    if (dismissClickOutside) {
                        dismiss()
                    }
                    return true
                }
            }
            return super.onTouchEvent(ev)
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            var width = MeasureSpec.getSize(widthMeasureSpec)
            var height = MeasureSpec.getSize(heightMeasureSpec)
            val lastInsets = this@BottomSheet.lastInsets
            if (lastInsets != null) {
                height -= lastInsets.getSystemWindowBottom()
                width -= lastInsets.getSystemWindowLeft() + lastInsets.getSystemWindowRight()
            }
            setMeasuredDimension(width, height)
            val isPortrait = width < height
            val widthSpec =
                when {
                    context.isTablet() -> {
                        MeasureSpec.makeMeasureSpec(
                            (minOf(context.displayMetrics.widthPixels, context.displayMetrics.heightPixels) * 0.8f).toInt(),
                            MeasureSpec.EXACTLY,
                        )
                    }
                    else -> {
                        MeasureSpec.makeMeasureSpec(
                            if (isPortrait) width else max((width * 0.6f).toInt(), min(dp(480f), width)),
                            MeasureSpec.EXACTLY,
                        )
                    }
                }
            sheetContainer.measure(widthSpec, MeasureSpec.makeMeasureSpec(height, AT_MOST))
        }

        override fun onLayout(
            changed: Boolean,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
        ) {
            val t = (bottom - top) - sheetContainer.measuredHeight
            var l = (right - left - sheetContainer.measuredWidth) / 2
            val lastInsets = this@BottomSheet.lastInsets
            if (lastInsets != null) {
                l += lastInsets.getSystemWindowLeft()
            }
            sheetContainer.layout(l, t, l + sheetContainer.measuredWidth, t + sheetContainer.measuredHeight)
        }
    }

    interface BottomSheetListener {
        fun onOpenAnimationStart()

        fun onOpenAnimationEnd()

        fun canDismiss(): Boolean
    }

    @Suppress("unused")
    open class BottomSheetListenerAdapter : BottomSheetListener {
        override fun onOpenAnimationStart() {
        }

        override fun onOpenAnimationEnd() {
        }

        override fun canDismiss() = false
    }

    init {
        window?.addFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
        )
        container.background = backDrawable
        container.fitsSystemWindows = true
        container.setOnApplyWindowInsetsListener { v, insets ->
            lastInsets = insets
            v.requestLayout()
            insets.consumeSystemWindowInsets()
        }
        container.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        backDrawable.alpha = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setWindowAnimations(R.style.DialogNoAnimation)
        setContentView(container, ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        sheetContainer.fitsSystemWindows = true
        sheetContainer.visibility = INVISIBLE
        container.addView(sheetContainer, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
        window?.let { window ->
            container.doOnPreDraw {
                SystemUIManager.lightUI(window, !context.isNightMode())
            }
        }
        if (customView != null) {
            if (customView!!.parent != null) {
                (customView!!.parent as ViewGroup).removeView(customView)
            }
            sheetContainer.addView(
                customView,
                FrameLayout.LayoutParams(
                    MATCH_PARENT,
                    if (customViewHeight > 0) customViewHeight else WRAP_CONTENT,
                    Gravity.BOTTOM,
                ),
            )
        }

        window?.let { window ->
            val params = window.attributes
            params.width = MATCH_PARENT
            params.height = MATCH_PARENT
            params.gravity = Gravity.START or Gravity.TOP
            params.dimAmount = 0f
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND.inv()
            if (!focusable) {
                params.flags = params.flags or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
            }
            window.attributes = params
        }
    }

    override fun show() {
        try {
            super.show()
        } catch (ignored: Exception) {
        }
        if (softInputResize) {
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        isDismissed = false
        cancelSheetAnimation()
        sheetContainer.measure(
            View.MeasureSpec.makeMeasureSpec(context.displayMetrics.widthPixels, AT_MOST),
            View.MeasureSpec.makeMeasureSpec(context.displayMetrics.heightPixels, AT_MOST),
        )
        if (isShown) return
        backDrawable.alpha = 0
        sheetContainer.translationY = sheetContainer.measuredHeight.toFloat()
        startAnimationRunnable =
            object : Runnable {
                override fun run() {
                    if (startAnimationRunnable != this || isDismissed) {
                        return
                    }
                    startAnimationRunnable = null
                    startOpenAnimation()
                }
            }
        sheetContainer.post(startAnimationRunnable)
    }

    private fun startOpenAnimation() {
        if (isDismissed) {
            return
        }
        sheetContainer.visibility = VISIBLE
        container.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        sheetContainer.translationY = sheetContainer.measuredHeight.toFloat()
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(sheetContainer, "translationY", 0f),
            ObjectAnimator.ofInt(backDrawable, "alpha", 153),
        )
        animatorSet.duration = 200
        animatorSet.startDelay = 20
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (curSheetAnimation != null && curSheetAnimation == animation) {
                        curSheetAnimation = null
                        bottomSheetListener?.onOpenAnimationEnd()
                        container.setLayerType(View.LAYER_TYPE_NONE, null)
                        isShown = true
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    if (curSheetAnimation != null && curSheetAnimation == animation) {
                        curSheetAnimation = null
                    }
                }
            },
        )
        animatorSet.start()
        curSheetAnimation = animatorSet
    }

    override fun dismiss() {
        if (isDismissed) {
            return
        }
        isDismissed = true
        isShown = false
        fakeDismiss(false)
    }

    fun fakeDismiss(
        fake: Boolean = true,
        doOnEnd: (() -> Unit)? = null,
    ) {
        cancelSheetAnimation()
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            ObjectAnimator.ofFloat(sheetContainer, "translationY", sheetContainer.measuredHeight.toFloat()),
            ObjectAnimator.ofInt(backDrawable, "alpha", 0),
        )
        animatorSet.duration = 180
        animatorSet.interpolator = AccelerateInterpolator()
        animatorSet.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (curSheetAnimation != null && curSheetAnimation == animation) {
                        curSheetAnimation = null
                        sheetContainer.post {
                            if (!fake) {
                                try {
                                    dismissInternal()
                                } catch (ignore: Exception) {
                                }
                            }
                            doOnEnd?.invoke()
                        }
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    if (curSheetAnimation != null && curSheetAnimation == animation) {
                        curSheetAnimation = null
                    }
                }
            },
        )
        animatorSet.start()
        curSheetAnimation = animatorSet
    }

    fun setListener(listener: BottomSheetListener) {
        bottomSheetListener = listener
    }

    fun setCustomView(view: View) {
        customView = view
    }

    fun getCustomView() = customView

    fun setCustomViewHeight(
        height: Int,
        endAction: (() -> Unit)? = null,
    ) {
        customViewHeight = height
        val params = customView?.layoutParams
        val duration =
            customView?.layoutParams.notNullWithElse(
                {
                    try {
                        min(abs(height - it.height) / speed, 200)
                    } catch (e: ArithmeticException) {
                        200
                    }
                },
                200,
            ).toLong()

        if (duration == 0L) {
            return
        }
        if (params != null) {
            val anim = ValueAnimator.ofInt(customView!!.height, height)
            anim.interpolator = LinearInterpolator()
            anim.addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                params.height = value
                customView?.layoutParams = params
                if (value == height) {
                    endAction?.let { it() }
                }
            }
            anim.duration = duration
            anim.start()
        }
    }

    fun setCustomViewHeightSync(height: Int) {
        customViewHeight = height
        customView?.layoutParams?.let { params ->
            params.height = height
            customView?.layoutParams = params
        }
    }

    fun getCustomViewHeight() = customViewHeight

    private fun cancelSheetAnimation() {
        curSheetAnimation?.cancel()
        curSheetAnimation = null
    }

    private fun dismissInternal() {
        try {
            super.dismiss()
        } catch (ignore: Exception) {
        }
    }

    class Builder {
        private var bottomSheet: BottomSheet

        constructor(context: Context) {
            bottomSheet = BottomSheet(context, focusable = false, softInputResize = true)
        }

        constructor(context: Context, needFocus: Boolean, softInputResize: Boolean) {
            bottomSheet = BottomSheet(context, needFocus, softInputResize)
        }

        fun setCustomView(view: View): Builder {
            bottomSheet.customView = view
            return this
        }

        fun setListener(bottomSheetListener: BottomSheetListener): Builder {
            bottomSheet.bottomSheetListener = bottomSheetListener
            return this
        }

        fun create(): BottomSheet = bottomSheet

        fun show(): BottomSheet {
            bottomSheet.show()
            return bottomSheet
        }
    }
}

fun BottomSheet.getMaxCustomViewHeight(): Int {
    val isNotchScreen = this.window?.isNotchScreen() ?: false
    val totalHeight =
        if (isNotchScreen) {
            val bottom = this.lastInsets?.getSystemWindowBottom() ?: 0
            context.realSize().y - bottom
        } else {
            val size = Point()
            val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            manager.defaultDisplay.getSize(size)
            size.y
        }
    return totalHeight - context.statusBarHeight()
}

fun buildBottomSheetView(
    context: Context,
    items: List<BottomSheetItem>,
): View {
    val linearLayout =
        LinearLayoutCompat(context).apply {
            orientation = LinearLayoutCompat.VERTICAL
        }
    val outValue = TypedValue()
    context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
    val itemHeight = context.dpToPx(56f)
    val padding = context.dpToPx(16f)
    items.forEachIndexed { index, bottomSheetItem ->
        val textView = TextView(context)
        if (index == 0) {
            textView.setBackgroundResource(R.drawable.bg_upper_round)
        } else {
            textView.setBackgroundColor(context.colorFromAttribute(R.attr.bg_white))
        }
        textView.foreground = ContextCompat.getDrawable(context, outValue.resourceId)
        bottomSheetItem.icon?.let {
            textView.setCompoundDrawables(it, null, null, null)
            textView.compoundDrawablePadding = padding
        }
        textView.text = bottomSheetItem.text
        textView.setTextColor(context.colorFromAttribute(R.attr.text_default))
        textView.setPadding(padding, 0, padding, 0)
        textView.gravity = Gravity.CENTER_VERTICAL
        textView.setOnClickListener {
            bottomSheetItem.clickAction.invoke()
        }
        linearLayout.addView(textView, ViewGroup.LayoutParams(MATCH_PARENT, itemHeight))
    }
    return linearLayout
}

data class BottomSheetItem(
    val text: String,
    val clickAction: () -> Unit,
    val icon: Drawable? = null,
)
