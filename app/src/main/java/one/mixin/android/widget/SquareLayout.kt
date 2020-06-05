package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import one.mixin.android.R
import org.jetbrains.anko.dip

class SquareLayout : FrameLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val paint: Paint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = context.dip(2).toFloat()
            color = resources.getColor(R.color.text_gray, null)
            pathEffect = DashPathEffect(floatArrayOf(20f, 20f), 0f)
        }
    }

    private val path = Path()
    private var rect: RectF? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = measuredWidth
        val h = measuredHeight
        val size = if (w > h) w else h
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY)
        )
        if (rect == null) {
            rect = RectF(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat())
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        rect?.let { path.addRect(it, Path.Direction.CCW) }
        canvas.drawPath(path, paint)
    }
}
