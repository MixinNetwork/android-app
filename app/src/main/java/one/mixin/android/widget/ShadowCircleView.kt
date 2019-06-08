package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.qr.CaptureActivity.Companion.SHOW_QR_CODE
import org.jetbrains.anko.dip

class ShadowCircleView : View {

    private val shadowHeight = dip(20).toFloat()
    private var radius = dip(20).toFloat()
    private var midX = 0f
    private var midY = 0f
    private var frameRect: RectF? = null
    private var circleRect: RectF? = null

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#979797")
        strokeWidth = dip(3f).toFloat()
    }

    private val framePaint = Paint()

    private val icon = ContextCompat.getDrawable(context, R.drawable.ic_qrcode)

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (framePaint.shader == null) {
            val x0 = width / 2f
            val y0 = radius
            val x1 = width / 2f
            val y1 = height.toFloat()
            val c0 = Color.TRANSPARENT
            val c1 = Color.WHITE
            framePaint.shader = LinearGradient(x0, y0, x1, y1, c0, c1, Shader.TileMode.CLAMP)

            midX = width / 2f
            midY = (height - shadowHeight) / 2
            frameRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            circleRect = RectF(midX - radius, 0f, midX + radius, height.toFloat())
            icon?.setBounds((midX - radius / 2).toInt(), (midY - radius / 2).toInt(), (midX + radius / 2).toInt(), (midY + radius / 2).toInt())
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPaint(framePaint)
        canvas.drawCircle(midX, midY, radius, ringPaint)
        if (context.defaultSharedPreferences.getBoolean(SHOW_QR_CODE, true)) {
            icon?.draw(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // only circle area respond touch event
        if (event.action == MotionEvent.ACTION_DOWN && !circleRect!!.contains(event.x, event.y)) {
            return false
        }
        return super.onTouchEvent(event)
    }
}