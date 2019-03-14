package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import one.mixin.android.R
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.ui.qr.CaptureFragment
import org.jetbrains.anko.dip

class ShadowCircleView : View {

    private val shadowHeight = dip(20).toFloat()
    private val drawableWidth = dip(82)
    private val drawableHeight = dip(80)
    private var midX = 0f
    private var midY = 0f
    private var frameRect: RectF? = null
    private var drawableRect: Rect? = null

    private val framePaint = Paint()

    private val captureDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_home_capture, null) }
    private val captureQRDrawable: Drawable by lazy { resources.getDrawable(R.drawable.ic_home_capture_qr, null) }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (framePaint.shader == null) {
            val x0 = width / 2f
            val x1 = width / 2f
            val y1 = height.toFloat()
            val c0 = Color.TRANSPARENT
            val c1 = Color.WHITE
            framePaint.shader = LinearGradient(x0, shadowHeight, x1, y1, c0, c1, Shader.TileMode.CLAMP)

            midX = width / 2f
            midY = (height - shadowHeight) / 2
            frameRect = RectF(0f, 0f, width.toFloat(), height.toFloat())

            val l = (width - drawableWidth) / 2
            captureDrawable.setBounds(l, 0, l + drawableWidth, drawableHeight)
            captureQRDrawable.setBounds(l, 0, l + drawableWidth, drawableHeight)
            drawableRect = Rect(l, 0, l + drawableWidth, drawableHeight)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawPaint(framePaint)
        if (context.defaultSharedPreferences.getBoolean(CaptureFragment.SHOW_QR_CODE, true)) {
            captureQRDrawable.draw(canvas)
        } else {
            captureDrawable.draw(canvas)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // only circle area respond touch event
        if (event.action == MotionEvent.ACTION_DOWN && drawableRect?.contains(event.x.toInt(), event.y.toInt()) == false) {
            return false
        }
        return super.onTouchEvent(event)
    }
}