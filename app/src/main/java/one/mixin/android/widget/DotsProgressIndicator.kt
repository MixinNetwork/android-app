package one.mixin.android.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.annotation.IntDef
import androidx.core.view.ViewCompat
import one.mixin.android.R
import timber.log.Timber
import java.math.BigDecimal

class DotsProgressIndicator : View {
    private var paints: Array<Paint> = arrayOf(Paint(), Paint(), Paint())
    private var colors: Array<Int> = arrayOf(Color.WHITE)
    private var dotCount = 3

    private var viewTop: Int = 0
    private var viewBottom: Int = 0
    private var viewStart: Int = 0
    private var viewEnd: Int = 0

    private var dotPadding: Float = 4f
    private var dotDiameter = 0F

    private lateinit var bitmaps: Array<Bitmap>
    private lateinit var canvases: Array<Canvas>
    private lateinit var offsets: Array<Int>

    private var anims: List<Animator>? = null
    private var set: AnimatorSet = AnimatorSet()

    private val lock: Any = Any()
    private var showRunnable: Runnable? = null

    private var totalDotToSpacing = 4f

    private var isRunning = false

    private var visibilityChangeDelay = 1000L
    private var retryDelay = 200L

    private var defaultWidth = BigDecimal(50)
    private var defaultHeight = BigDecimal(30)

    private var mode = MODE_BEST_FIT

    constructor(context: Context) : super(context) {
        setup(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setup(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setup(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Suppress("unused")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setup(context, attrs)
    }

    fun setup(context: Context, attrs: AttributeSet?) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.DotsProgressIndicator, 0, 0)
        try {
            var outValue = TypedValue()
            ta.getValue(R.styleable.DotsProgressIndicator_dotsMode, outValue)
            mode = outValue.data

            dotCount = ta.getInt(R.styleable.DotsProgressIndicator_numberOfDots, dotCount)
            visibilityChangeDelay = ta.getInt(R.styleable.DotsProgressIndicator_visibilityChangeDelay, visibilityChangeDelay.toInt()).toLong()
            dotPadding = (context.resources.displayMetrics.density * dotPadding)

            outValue = TypedValue()
            ta.getValue(R.styleable.DotsProgressIndicator_dotColors, outValue)
            val colorsArrayResId = outValue.resourceId

            if (mode == MODE_SIZE_PROVIDED) {
                dotPadding = ta.getDimension(R.styleable.DotsProgressIndicator_dotsSpacing, dotPadding)
                dotDiameter = ta.getDimension(R.styleable.DotsProgressIndicator_dotsDiameter, dotDiameter)
            }

            try {
                if (isInEditMode || colorsArrayResId == 0) {
                    colors = Array(dotCount) { i ->
                        colors[i % colors.size]
                    }
                } else {
                    val colorType = context.resources.obtainTypedArray(colorsArrayResId)
                    colors = Array(dotCount) { i ->
                        colorType.getColor(i, colors[i % colors.size])
                    }
                    colorType.recycle()
                }
            } catch (t: Throwable) {
                Timber.tag("DOTS").e(t)
            }
        } finally {
            ta.recycle()
        }

        paints = Array(dotCount) { i ->
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            p.color = colors[i % dotCount]
            p
        }

        offsets = Array(dotCount) {
            0
        }

        defaultWidth = defaultWidth.multiply(BigDecimal(context.resources.displayMetrics.density.toDouble()))
        defaultHeight = defaultHeight.multiply(BigDecimal(context.resources.displayMetrics.density.toDouble()))
        totalDotToSpacing = dotPadding * (dotCount - 1)

        if (mode == MODE_SIZE_PROVIDED) {
            dotPadding = (context.resources.displayMetrics.density * dotPadding) // should come from attr or this as default
            totalDotToSpacing = dotPadding * (dotCount - 1)
            // should come from attrs or default value already set
            defaultWidth = defaultWidth.multiply(BigDecimal(context.resources.displayMetrics.density.toDouble()))
            defaultHeight = defaultHeight.multiply(BigDecimal(context.resources.displayMetrics.density.toDouble()))
        }
    }

    private fun setupBitmaps() {
        bitmaps = Array(dotCount) {
            val bmpWidth = ((width - totalDotToSpacing.toInt() - getPaddingStartCompat() - getPaddingEndCompat()) / dotCount)
            Bitmap.createBitmap(bmpWidth, bmpWidth, Bitmap.Config.ARGB_4444)
        }
        canvases = Array(dotCount) { i ->
            val b = bitmaps[i]
            val c = Canvas(b)
            val cx = BigDecimal(b.width).divide(BigDecimal(2)).toFloat()
            c.drawCircle(cx, cx, cx, paints[i])
            c
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val calculatedWidth = MeasureSpec.getSize(widthMeasureSpec)

        when (widthMode) {
            MeasureSpec.UNSPECIFIED -> Timber.tag("DOTS").d("Measure UNSPECIFIED")
            MeasureSpec.AT_MOST -> Timber.tag("DOTS").d("Measure AT_MOST")
            MeasureSpec.EXACTLY -> Timber.tag("DOTS").d("Measure EXACTLY")
        }

        if (mode == MODE_SIZE_PROVIDED) {
            val totalWidth = (dotDiameter * dotCount) + totalDotToSpacing
            val msw = MeasureSpec.makeMeasureSpec(totalWidth.toInt(), MeasureSpec.EXACTLY)
            val bmpWidth = ((totalWidth.toInt() - totalDotToSpacing.toInt() - getPaddingStartCompat() - getPaddingEndCompat()) / dotCount)
            val msh = MeasureSpec.makeMeasureSpec(bmpWidth * 2, MeasureSpec.EXACTLY)
            super.onMeasure(msw, msh)
        } else if (widthMode == MeasureSpec.UNSPECIFIED || widthMode == MeasureSpec.AT_MOST) {
            val msw = MeasureSpec.makeMeasureSpec(defaultWidth.toInt(), MeasureSpec.EXACTLY)
            val bmpWidth = ((defaultWidth.toInt() - totalDotToSpacing.toInt() - getPaddingStartCompat() - getPaddingEndCompat()) / dotCount)
            val msh = MeasureSpec.makeMeasureSpec(bmpWidth * 2, MeasureSpec.EXACTLY)
            super.onMeasure(msw, msh)
        } else {
            val bmpWidth = ((calculatedWidth - totalDotToSpacing.toInt() - getPaddingStartCompat() - getPaddingEndCompat()) / dotCount)
            val msh = MeasureSpec.makeMeasureSpec(bmpWidth * 2, MeasureSpec.EXACTLY)
            super.onMeasure(widthMeasureSpec, msh)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewTop = paddingTop
        viewBottom = h - paddingBottom
        viewEnd = w - getPaddingEndCompat()
        viewStart = getPaddingStartCompat()

        setupBitmaps()

        if (visibility == View.VISIBLE && !isRunning) {
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        bitmaps.forEachIndexed { i, it ->
            val space = dotPadding * i
            val xadd = (i * it.width)
            val x = viewStart.toFloat() + xadd + space
            var y = (viewBottom - bitmaps[i].height).toFloat()
            y -= offsets[i]
            canvas.drawBitmap(it, x, y, paints[i])
        }
    }

    private fun start(): Boolean {
        synchronized(lock) {
            try {
                // hack as some times the bitamaps
                // are not ready when rendering
                bitmaps[0].height
            } catch (t: Throwable) {
                return false
            }
            anims = List<Animator>(dotCount) { i ->
                val h = bitmaps[0].height
                val va = ValueAnimator.ofInt(viewTop, h, viewTop)
                va.addUpdateListener {
                    offsets[i] = it.animatedValue as Int
//                    if (i == 0)
//                        Log.d("DotsValueAnimInt", "Num = ${offsets[i]}")
                    invalidate()
                }
                va.interpolator = DecelerateInterpolator()
                va.startDelay = if (i == 0) 0 else (600L / dotCount) * i
                va.duration = 300L
                va.repeatCount = ValueAnimator.INFINITE
                va
            }

            set = AnimatorSet()
            set.duration = 900L
            set.playTogether(anims)
            set.addListener(object : AbsAnimatorListener {
                override fun onAnimationEnd(animator: Animator?) {
                    super.onAnimationEnd(animator)
                    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                        return
                    }
                    if (visibility == View.VISIBLE) {
                        post { start() }
                    }
                }

                override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                    super.onAnimationEnd(animation, isReverse)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                        return
                    }
                    if (visibility == View.VISIBLE) {
                        post { start() }
                    }
                }
            })
            set.start()
            isRunning = true
            return true
        }
    }

    private fun stop() {
        synchronized(lock) {
            isRunning = false
        }
    }

    override fun setVisibility(visibility: Int) {
        if (visibility == getVisibility()) return

        synchronized(lock) {
            if (showRunnable != null) {
                visibilityHandler.removeCallbacks(showRunnable)
                showRunnable = null
            }
            showRunnable = Runnable {
                super.setVisibility(visibility)
                var success = true
                if (visibility == View.VISIBLE) {
                    success = start()
                } else {
                    stop()
                }
                if (success) {
                    showRunnable = null
                } else {
                    visibilityHandler.postDelayed(showRunnable, retryDelay)
                }
            }
            visibilityHandler.postDelayed(showRunnable, visibilityChangeDelay)
        }
    }

    override fun onDetachedFromWindow() {
        synchronized(lock) {
            if (showRunnable != null) {
                visibilityHandler.removeCallbacks(showRunnable)
                showRunnable = null
            }
        }
        super.onDetachedFromWindow()
    }

    private fun getPaddingStartCompat(): Int {
        return ViewCompat.getPaddingStart(this)
    }

    private fun getPaddingEndCompat(): Int {
        return ViewCompat.getPaddingEnd(this)
    }

    companion object {
        val visibilityHandler: Handler = Handler()

        @IntDef(MODE_BEST_FIT, MODE_SIZE_PROVIDED)
        @Retention(AnnotationRetention.SOURCE)
        annotation class Mode

        private const val MODE_BEST_FIT = 0
        private const val MODE_SIZE_PROVIDED = 1
    }
}

interface AbsAnimatorListener : Animator.AnimatorListener {
    override fun onAnimationRepeat(animator: Animator?) {
    }

    override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
    }

    override fun onAnimationEnd(animator: Animator?) {
    }

    override fun onAnimationCancel(animator: Animator?) {
    }

    override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
    }

    override fun onAnimationStart(animator: Animator?) {
    }
}
