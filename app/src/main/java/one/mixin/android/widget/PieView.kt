package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import org.jetbrains.anko.dip

class PieView : View {

    companion object {
        val COLOR_DEFAULT = Color.parseColor("#F5F5F5")
        val COLOR_FIRST = Color.parseColor("#005EE4")
        val COLOR_SECOND = Color.parseColor("#3387FF")
        val COLOR_THIRD = Color.parseColor("#70BEFF")

        const val INTERVAL = 12
    }

    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = COLOR_FIRST
    }
    private val arcInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private var circleWidth = dip(60).toFloat()
    private val startAngle = -90f
    private var firstSweepAngle = 0f
    private var secondSweepAngle = 0f
    private var thirdSweepAngle = 0f
    private var curSweepAngle = 0f
    private var arcRect: RectF? = null
    private var midX = 0f
    private var midY = 0f

    private var pies: List<PieItem>? = null
    private var firstAngle: Float = 0f
    private var secondAngle: Float = 0f

    private var animating = false

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet)

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        midX = width / 2f
        midY = height / 2f
        if (arcRect == null) {
            arcRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            circleWidth = width * .3f
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        arcRect?.let { arcRect ->
            if (pies == null || pies!!.isEmpty()) {
                arcPaint.color = COLOR_DEFAULT
                canvas.drawArc(arcRect, startAngle, 360f, true, arcPaint)
                canvas.drawCircle(midX, midY, circleWidth, arcInnerPaint)
                return
            }

            when {
                curSweepAngle <= firstAngle -> {
                    arcPaint.color = COLOR_FIRST
                    canvas.drawArc(arcRect, startAngle, firstSweepAngle, true, arcPaint)
                    firstSweepAngle += INTERVAL
                }
                if (pies!!.size == 2) curSweepAngle <= 360 + INTERVAL else curSweepAngle <= firstAngle + secondAngle -> {
                    arcPaint.color = COLOR_FIRST
                    canvas.drawArc(arcRect, startAngle, firstAngle, true, arcPaint)
                    arcPaint.color = COLOR_SECOND
                    canvas.drawArc(arcRect, startAngle + firstAngle, secondSweepAngle, true, arcPaint)
                    secondSweepAngle += INTERVAL
                    if (firstAngle + secondSweepAngle > 360) {
                        secondSweepAngle = 360 - firstAngle
                    }
                }
                else -> {
                    arcPaint.color = COLOR_FIRST
                    canvas.drawArc(arcRect, startAngle, firstAngle, true, arcPaint)
                    arcPaint.color = COLOR_SECOND
                    canvas.drawArc(arcRect, startAngle + firstAngle, secondAngle, true, arcPaint)
                    arcPaint.color = COLOR_THIRD
                    canvas.drawArc(arcRect, startAngle + firstAngle + secondAngle, thirdSweepAngle, true, arcPaint)
                    thirdSweepAngle += INTERVAL
                    if (firstAngle + secondAngle + thirdSweepAngle > 360) {
                        thirdSweepAngle = 360 - firstAngle - secondAngle
                    }
                }
            }
        }
        curSweepAngle += INTERVAL
        canvas.drawCircle(midX, midY, circleWidth, arcInnerPaint)
        animating = if (curSweepAngle <= 360 + INTERVAL) {
            invalidate()
            true
        } else {
            false
        }
    }

    fun setPieItem(list: List<PieItem>?, needAnim: Boolean) {
        if (list == null) return

        this.pies = list

        if (!needAnim) return

        if (animating) return

        if (list.isNotEmpty()) {
            val first = pies!![0].percent
            firstAngle = first * 360
            if (pies!!.size >= 2) {
                val second = pies!![1].percent
                secondAngle = second * 360
            }
            curSweepAngle = 0f
            firstSweepAngle = 0f
            secondSweepAngle = 0f
            thirdSweepAngle = 0f
        }
        invalidate()
    }

    data class PieItem(val name: String, val percent: Float)
}
