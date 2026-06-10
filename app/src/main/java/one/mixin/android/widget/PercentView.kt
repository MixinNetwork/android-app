package one.mixin.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.view.View
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.forEachWithIndex
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode

class PercentView : View {
    private val colorBlue by lazy { context.getColor(R.color.wallet_blue) }
    private val colorBlueDark by lazy { context.getColor(R.color.wallet_blue_dark) }
    private val colorYellow by lazy { context.getColor(R.color.wallet_yellow) }
    private val colorYellowDark by lazy { context.getColor(R.color.wallet_yellow_dark) }
    private val colorPurple by lazy { context.getColor(R.color.wallet_purple) }
    private val colorPurpleDark by lazy { context.getColor(R.color.wallet_purple_dark) }

    private val shadowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter =
                ColorMatrixColorFilter(
                    ColorMatrix().apply {
                        setScale(SHADOW_SCALE_RGB, SHADOW_SCALE_RGB, SHADOW_SCALE_RGB, SHADOW_SCALE_ALPHA)
                    },
                )
        }
    private val strokeHeight = context.dpToPx(4f)
    private val radius = context.dpToPx(2f).toFloat()
    private var percents = arrayListOf<Float>()
    private val shadowBounds = RectF()
    private var blurShadow: Bitmap? = null
    private var blueGradient: GradientDrawable? = null
    private var purpleGradient: GradientDrawable? = null
    private var yellowGradient: GradientDrawable? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (percents.isEmpty()) return

        if (blurShadow != null && !blurShadow!!.isRecycled) {
            val saveCount = canvas.save()
            canvas.translate(0f, strokeHeight.toFloat())
            canvas.drawBitmap(blurShadow!!, 0f, 0f, shadowPaint)
            canvas.restoreToCount(saveCount)
        }

        blueGradient?.draw(canvas)
        purpleGradient?.draw(canvas)
        yellowGradient?.draw(canvas)
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        createShadowByData()
    }

    fun setPercents(list: List<PercentItem>) {
        if (list.isEmpty()) return

        percents.clear()
        var pre = 0f
        list.forEachWithIndex { i, percentItem ->
            if (i == 0 || i == 1) {
                percents.add(percentItem.percent)
                pre += percentItem.percent
            } else if (i == 2) {
                percents.add(1f - pre)
            }
        }

        if (createShadowByData()) return
        invalidate()
    }

    private fun createShadowByData(): Boolean {
        var startW = paddingStart
        percents.forEachWithIndex { i, p ->
            val w = (width * p).toInt()
            when (i) {
                0 -> {
                    blueGradient =
                        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(colorBlue, colorBlueDark)).apply {
                            cornerRadii = getCornerRadii(i, percents.size)
                            bounds.set(startW, strokeHeight, startW + w, strokeHeight * 2)
                        }
                }
                1 -> {
                    purpleGradient =
                        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(colorPurple, colorPurpleDark)).apply {
                            cornerRadii = getCornerRadii(i, percents.size)
                            bounds.set(startW, strokeHeight, startW + w, strokeHeight * 2)
                        }
                }
                else -> {
                    yellowGradient =
                        GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(colorYellow, colorYellowDark)).apply {
                            cornerRadii = getCornerRadii(i, percents.size)
                            bounds.set(startW, strokeHeight, startW + w, strokeHeight * 2)
                        }
                }
            }
            startW += w
        }
        if (width <= 0 || height <= 0) {
            return true
        }

        shadowBounds.set(radius / 2, 0f, width.toFloat(), height.toFloat())
        if (blurShadow == null) {
            blurShadow =
                Bitmap.createBitmap(
                    shadowBounds.width().toInt(),
                    (shadowBounds.height() * 1.5f).toInt(),
                    Bitmap.Config.ARGB_8888,
                )
        } else {
            blurShadow?.eraseColor(Color.TRANSPARENT)
        }
        try {
            val c = Canvas()
            createShadow(blurShadow, c, context.dpToPx(8f).toFloat())
        } catch (throwable: Throwable) {
            Timber.e(throwable)
        }
        return false
    }

    private fun createShadow(
        bitmap: Bitmap?,
        canvas: Canvas,
        blurRadius: Float,
    ) {
        canvas.setBitmap(bitmap)
        canvas.translate(0f, -strokeHeight.toFloat())
        blueGradient?.draw(canvas)
        purpleGradient?.draw(canvas)
        yellowGradient?.draw(canvas)
        val rs = getRS(context)
        val blur = getBlur(context)
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)
        blur.setRadius(blurRadius.coerceIn(0f, 25f))
        blur.setInput(input)
        blur.forEach(output)
        output.copyTo(bitmap)
        input.destroy()
        output.destroy()
    }

    private fun getCornerRadii(
        index: Int,
        size: Int,
    ): FloatArray {
        return when (size) {
            1 -> floatArrayOf(radius, radius, radius, radius, radius, radius, radius, radius)
            2 -> {
                when (index) {
                    0 -> floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
                    else -> floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
                }
            }
            else -> {
                when (index) {
                    0 -> floatArrayOf(radius, radius, 0f, 0f, 0f, 0f, radius, radius)
                    1 -> floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                    else -> floatArrayOf(0f, 0f, radius, radius, radius, radius, 0f, 0f)
                }
            }
        }
    }

    companion object {
        private const val SHADOW_SCALE_RGB = 0.85f
        private const val SHADOW_SCALE_ALPHA = 0.6f

        private var rs: RenderScript? = null

        fun getRS(context: Context): RenderScript {
            if (rs == null) {
                rs = RenderScript.create(context.applicationContext)
            }
            return rs!!
        }

        private var blur: ScriptIntrinsicBlur? = null

        fun getBlur(context: Context): ScriptIntrinsicBlur {
            if (blur == null) {
                val rs = getRS(context)
                blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            }
            return blur!!
        }
    }

    data class PercentItem(val name: String, val percent: Float)
}

fun BigDecimal.calcPercent(totalUSD: BigDecimal): Float =
    (this.divide(totalUSD, 16, RoundingMode.HALF_UP)).setScale(2, RoundingMode.DOWN).toFloat()
