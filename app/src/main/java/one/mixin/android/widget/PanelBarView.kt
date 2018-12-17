package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import one.mixin.android.extension.dpToPx
import kotlin.math.abs

class PanelBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barWidth by lazy { context.dpToPx(36f).toFloat() }
    private val barHeight by lazy { context.dpToPx(6f).toFloat() }

    private var offsetHorizontal: Float = 0f
    private var offsetVertical: Float = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DADEE5")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = context.dpToPx(4f).toFloat()
    }

    private var currHeight = height.toFloat()
        set(value) {
            if (value == field) return
            field = value
            setPath(path)
        }
    private val path = Path()

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    var maxDragDistance = 0f

    override fun onDraw(canvas: Canvas) {
        if (offsetHorizontal == 0f) {
            offsetHorizontal = (width - barWidth) / 2
        }
        if (offsetVertical == 0f) {
            offsetVertical = (height - barHeight) / 2
        }
        if (currHeight == 0f) {
            currHeight = height.toFloat()
        }
        canvas.drawPath(path, paint)
    }

    private var originY: Float = 0f
    private var downY: Float = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downY = event.rawY
                originY = event.rawY

                callback?.onTap()
                if (!postDelayed(performClick, 300)) {
                    callback?.onClick()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val moveY = event.rawY
                val disY = moveY - downY

                if ((abs(originY - moveY) > touchSlop)) {
                    removeCallbacks(performClick)
                }

                val projectionDisY = disY / height
                currHeight -= projectionDisY
                callback?.onDrag(disY)
                downY = moveY
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val ratio = currHeight / maxDragDistance
                currHeight = if (ratio < .5f) {
                    height.toFloat()
                } else {
                    offsetVertical
                }
                downY = 0f
                originY = 0f
                callback?.onRelease()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun setPath(path: Path) {
        path.apply {
            rewind()
            moveTo(offsetHorizontal, offsetVertical)
            lineTo(width / 2f, currHeight - offsetVertical)
            lineTo(width.toFloat() - offsetHorizontal, offsetVertical)
        }
        invalidate()
    }

    private val performClick = Runnable { callback?.onClick() }

    var callback: Callback? = null

    interface Callback {
        fun onDrag(dis: Float)
        fun onRelease()
        fun onClick()
        fun onTap()
    }
}