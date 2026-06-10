package one.mixin.android.widget.gallery.internal.ui.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader.TileMode
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class SquareImageView : AppCompatImageView {
    private val mBitmapPaint: Paint = Paint()
    private val mMatrix: Matrix = Matrix()

    private var mBitmapShader: BitmapShader? = null
    private var mWidth: Int = 0
    private var mRoundRect: RectF? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    init {
        mBitmapPaint.isAntiAlias = true
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = Math.min(measuredWidth, measuredHeight)
        setMeasuredDimension(mWidth, mWidth)
    }

    private fun setUpShader() {
        val drawable = drawable ?: return
        val bmp = drawableToBitmap(drawable) ?: return

        mBitmapShader = BitmapShader(bmp, TileMode.CLAMP, TileMode.CLAMP)
        val bSize = Math.min(bmp.width, bmp.height)
        val scale = mWidth * 1.0f / bSize
        mMatrix.setScale(scale, scale)
        mBitmapShader!!.setLocalMatrix(mMatrix)
        mBitmapPaint.shader = mBitmapShader
    }

    override fun onDraw(canvas: Canvas) {
        if (drawable == null) {
            return
        }
        setUpShader()
        canvas.drawRoundRect(mRoundRect!!, 0f, 0f, mBitmapPaint)
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        mRoundRect = RectF(0f, 0f, w.toFloat(), h.toFloat())
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        val w = drawable.intrinsicWidth
        val h = drawable.intrinsicHeight
        if (w <= 0 || h <= 0) {
            return null
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, w, h)
        drawable.draw(canvas)
        return bitmap
    }
}
