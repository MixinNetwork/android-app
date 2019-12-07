package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Path
import android.util.AttributeSet
import android.widget.RelativeLayout
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute

open class UpperRoundRelativeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RelativeLayout(context, attrs, defStyle) {

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(roundedRect(roundRadius, roundRadius), paint)
    }

    private var roundRadius = 0f

    fun setRoundRadius(roundRadius: Float) {
        this.roundRadius = roundRadius
        invalidate()
    }

    fun setRoundRadius(roundRadius: Int) {
        setRoundRadius(roundRadius.toFloat())
    }

    private val paint by lazy {
        Paint(ANTI_ALIAS_FLAG).apply {
            color = context.colorFromAttribute(R.attr.bg_white)
            style = Paint.Style.FILL
        }
    }

    private fun roundedRect(
        rx: Float,
        ry: Float
    ): Path {
        val path = Path()
        path.moveTo(right.toFloat(), top + ry)
        path.rQuadTo(0f, -ry, -rx, -ry)
        path.rLineTo(-(width - (2 * rx)), 0f)
        path.rQuadTo(-rx, 0f, -rx, ry)
        path.rLineTo(0f, height.toFloat())
        path.rLineTo(width.toFloat(), 0f)
        path.close()
        return path
    }
}
