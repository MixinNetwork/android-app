package one.mixin.android.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import one.mixin.android.extension.screenHeight
import one.mixin.android.extension.screenWidth
import timber.log.Timber

abstract class PorterImageView : AppCompatImageView {
    private var maskCanvas: Canvas? = null
    private var maskBitmap: Bitmap? = null
    private lateinit var maskPaint: Paint
    private lateinit var erasePaint: Paint

    private var drawableCanvas: Canvas? = null
    private var drawableBitmap: Bitmap? = null
    private var drawablePaint: Paint? = null

    private var invalidated = true

    constructor(context: Context) : super(context) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setup()
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        setup()
    }

    private fun setup() {
        if (scaleType == ImageView.ScaleType.FIT_CENTER) {
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        maskPaint.color = Color.BLACK

        erasePaint = Paint()
        erasePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    override fun invalidate() {
        invalidated = true
        super.invalidate()
    }

    override fun onSizeChanged(
        w: Int,
        h: Int,
        oldw: Int,
        oldh: Int,
    ) {
        super.onSizeChanged(w, h, oldw, oldh)
        createMaskCanvas(w, h, oldw, oldh)
    }

    protected fun createMaskCanvas() {
        maskCanvas?.let {
            val width = it.width
            val height = it.height
            maskCanvas = null
            createMaskCanvas(width, height)
        }
    }

    private fun createMaskCanvas(
        width: Int,
        height: Int,
        oldw: Int = 0,
        oldh: Int = 0,
    ) {
        val sizeChanged = width != oldw || height != oldh
        val isValid = width > 0 && height > 0 && width < context.screenWidth() && height < context.screenHeight()
        if (isValid && (maskCanvas == null || sizeChanged)) {
            maskCanvas = Canvas()
            maskBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            maskCanvas!!.setBitmap(maskBitmap)

            maskPaint.reset()
            paintMaskCanvas(maskCanvas!!, maskPaint, width, height)

            drawableCanvas = Canvas()
            drawableBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            drawableCanvas!!.setBitmap(drawableBitmap)
            drawablePaint = Paint(Paint.ANTI_ALIAS_FLAG)
            invalidated = true
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE) {
            if (maskBitmap == null || drawableBitmap == null) {
                createMaskCanvas()
            }
        } else {
            drawableBitmap?.recycle()
            maskBitmap?.recycle()
            drawableBitmap = null
            maskBitmap = null
        }
    }

    protected abstract fun paintMaskCanvas(
        maskCanvas: Canvas,
        maskPaint: Paint,
        width: Int,
        height: Int,
    )

    override fun onDraw(canvas: Canvas) {
        if (!isInEditMode) {
            val saveCount = canvas.saveLayer(0.0f, 0.0f, width.toFloat(), height.toFloat(), null)
            try {
                if (invalidated) {
                    val drawable = drawable
                    if (drawable != null) {
                        invalidated = false
                        val imageMatrix = imageMatrix
                        if (imageMatrix == null) {
                            drawableCanvas?.let {
                                drawable.draw(it)
                            }
                        } else {
                            drawableCanvas?.let {
                                val drawableSaveCount = it.saveCount
                                it.save()
                                it.concat(imageMatrix)
                                it.drawPaint(erasePaint)
                                drawable.draw(it)
                                it.restoreToCount(drawableSaveCount)
                            }
                        }

                        drawablePaint?.reset()
                        drawablePaint?.isFilterBitmap = false
                        drawablePaint?.xfermode = PORTER_DUFF_XFERMODE
                        drawableCanvas?.drawBitmap(maskBitmap!!, 0.0f, 0.0f, drawablePaint)
                    }
                }
                if (!invalidated) {
                    drawablePaint?.xfermode = null
                    drawableBitmap?.let {
                        canvas.drawBitmap(it, 0.0f, 0.0f, drawablePaint)
                    }
                }
            } catch (e: Exception) {
                val log = "Exception occured while drawing $id"
                Timber.e(e, log)
            } finally {
                canvas.restoreToCount(saveCount)
            }
        } else {
            super.onDraw(canvas)
        }
    }

    companion object {
        private val TAG = PorterImageView::class.java.simpleName

        private val PORTER_DUFF_XFERMODE = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
}
