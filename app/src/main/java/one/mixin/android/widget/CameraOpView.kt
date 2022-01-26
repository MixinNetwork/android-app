package one.mixin.android.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import one.mixin.android.R
import one.mixin.android.extension.dp

class CameraOpView : View, GestureDetector.OnGestureListener {

    private enum class Mode {
        NONE,
        EXPAND,
        PROGRESS
    }

    private var ringColor = Color.WHITE
    private var circleColor = context.getColor(R.color.colorDarkBlue)
    private var ringStrokeWidth = 5.dp.toFloat()
    @Suppress("unused")
    private var progressStrokeWidth = 5f.dp.toFloat()
    private var circleWidth = -10f // initial value less than 0 for delay
    private var maxCircleWidth = 0f
    private var circleInterval = 3f
    private var progressStartAngle = -90f
    private var curSweepAngle = 0f
    private var progressRect: RectF? = null
    private var expand = 1.1f
    private var radius = 0f
    private var rawRadius = 0f
    private var midX = 0f
    private var midY = 0f
    private var progressInterval = 0f

    private var mode = Mode.NONE

    var time = 0f

    private val gestureDetector = GestureDetector(context, this)
    private var callback: CameraOpCallback? = null

    private val mHandler =
        object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == 1) {
                    curSweepAngle += progressInterval
                    if (curSweepAngle > 360) {
                        curSweepAngle = 360f
                        invalidate()
                        stop()
                        time = curSweepAngle / progressInterval / 10
                        callback?.onProgressStop(time)
                        clean()
                    } else {
                        invalidate()
                    }
                }
            }
        }

    private val task =
        object : Runnable {
            override fun run() {
                mHandler.sendEmptyMessage(1)
                mHandler.postDelayed(this, 100)
            }
        }

    private fun start() {
        task.run()
    }

    private fun stop() {
        mHandler.removeCallbacks(task)
    }

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ringColor
        strokeWidth = ringStrokeWidth
    }

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = circleColor
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun animateValue(form: Float, to: Float) {
        val anim = ValueAnimator.ofFloat(form, to)
        anim.addUpdateListener { valueAnimator ->
            radius = valueAnimator.animatedValue as Float * rawRadius
            invalidate()
        }
        anim.duration = 200
        anim.start()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (progressRect == null) {
            val size = width / 1.5f - ringStrokeWidth
            rawRadius = size / 2
            radius = rawRadius
            maxCircleWidth = radius - ringStrokeWidth
            midX = width / 2f
            midY = width / 2f
            val maxRadius = rawRadius * expand
            progressRect = RectF(midX - maxRadius, midY - maxRadius, midX + maxRadius, midY + maxRadius)
        }
    }

    override fun onDraw(canvas: Canvas) {
        ringPaint.color = ringColor
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
            canvas.drawCircle(midX, midY, radius, ringPaint)
            ringPaint.color = circleColor
            progressRect?.let { progressRect ->
                canvas.drawArc(progressRect, progressStartAngle, curSweepAngle, false, ringPaint)
            }
            canvas.drawCircle(midX, midY, maxCircleWidth, circlePaint)
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
                    stop()
                    time = curSweepAngle / progressInterval / 10
                    callback?.onProgressStop(time)
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
        animateValue(expand, 1f)
        clean()
        callback?.onClick()
        return true
    }

    override fun onDown(e: MotionEvent?): Boolean {
        animateValue(1f, expand)
        mode = Mode.EXPAND
        return true
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
        mode = Mode.PROGRESS
        callback?.readyForProgress()
    }

    private fun clean() {
        mode = Mode.NONE
        curSweepAngle = 0f
        circleWidth = -10f
        radius = rawRadius
        invalidate()
    }

    fun startProgress() {
        start()
    }

    fun setMaxDuration(duration: Int) {
        progressInterval = 360f / duration / 10
    }

    fun setCameraOpCallback(callback: CameraOpCallback) {
        this.callback = callback
    }

    fun isRecording() = mode == Mode.PROGRESS

    interface CameraOpCallback {
        fun onClick()
        fun readyForProgress()
        fun onProgressStop(time: Float)
    }
}
