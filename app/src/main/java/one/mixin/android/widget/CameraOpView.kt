package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringSystem
import org.jetbrains.anko.dip

class CameraOpView : View, GestureDetector.OnGestureListener {

    private enum class Mode {
        NONE,
        EXPAND,
        PROGRESS
    }

    private var ringColor = Color.WHITE
    private var circleColor = Color.RED
    private var ringStrokeWidth = dip(5).toFloat()
    private var progressStrokeWidth = dip(4.5f).toFloat()
    private var circleWidth = -10f // initial value less than 0 for delay
    private var maxCircleWidth = 0f
    private var circleInterval = 3f
    private var progressInterval = 2f
    private var progressStartAngle = -90f
    private var curSweepAngle = 0f
    private var progressRect: RectF? = null
    private var expand = 1.2f
    private var radius = 0f
    private var rawRadius = 0f
    private var midX = 0f
    private var midY = 0f

    private var mode = Mode.NONE

    private val gestureDetector = GestureDetector(context, this)
    private var callback: CameraOpCallback? = null
    private val spring = SpringSystem.create().createSpring().apply {
        springConfig = SpringConfig.fromOrigamiTensionAndFriction(80.0, 4.0)
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ringColor
        strokeWidth = ringStrokeWidth
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = circleColor
        strokeWidth = progressStrokeWidth
        strokeCap = Paint.Cap.ROUND
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = circleColor
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        spring.addListener(object : SimpleSpringListener() {
            override fun onSpringUpdate(spring: Spring) {
                val value = spring.currentValue
                if (value < .8) {
                    return
                }
                radius = value.toFloat() * rawRadius
                invalidate()
            }
        })
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (progressRect == null) {
            val size = width / 1.5f - ringStrokeWidth
            val offset = (width - size * expand) / 2
            progressRect = RectF(offset, offset, width - offset, width - offset)
            rawRadius = size / 2
            radius = rawRadius
            maxCircleWidth = radius - ringStrokeWidth
            midX = width / 2f
            midY = width / 2f
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (mode == Mode.NONE) {
            canvas.drawCircle(midX, midY, radius, ringPaint)
        } else if (mode == Mode.EXPAND) {
            canvas.drawCircle(midX, midY, radius, ringPaint)
            canvas.drawCircle(midX, midY, circleWidth, circlePaint)
            circleWidth += circleInterval
            if (circleWidth <= maxCircleWidth) {
                invalidate()
            }
        } else {
            canvas.drawCircle(midX, midY, radius * expand, ringPaint)
            canvas.drawArc(progressRect, progressStartAngle, curSweepAngle, false, progressPaint)
            canvas.drawCircle(midX, midY, maxCircleWidth, circlePaint)
            curSweepAngle += progressInterval
            if (curSweepAngle <= 360 + progressInterval) {
                invalidate()
            } else {
                callback?.onProgressStop()
                clean()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val handled = gestureDetector.onTouchEvent(event)
        if (handled) {
            return true
        }
        when (event.action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (mode == Mode.PROGRESS) {
                    callback?.onProgressStop()
                }
                clean()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onShowPress(e: MotionEvent?) {
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        spring.endValue = 1.0
        clean()
        callback?.onClick()
        return true
    }

    override fun onDown(e: MotionEvent?): Boolean {
//        spring.endValue = 1.2
//        mode = Mode.EXPAND
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
//        spring.endValue = 1.0
//        mode = Mode.PROGRESS
//        invalidate()
//        callback?.onProgressStart()
    }

    private fun clean() {
        mode = Mode.NONE
        curSweepAngle = 0f
        circleWidth = -10f
        radius = rawRadius
        invalidate()
    }

    fun setCameraOpCallback(callback: CameraOpCallback) {
        this.callback = callback
    }

    interface CameraOpCallback {
        fun onClick()
        fun onProgressStart()
        fun onProgressStop()
    }
}