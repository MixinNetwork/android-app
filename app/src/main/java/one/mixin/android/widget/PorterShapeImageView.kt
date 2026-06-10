package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.collection.ArrayMap

class PorterShapeImageView : PorterImageView {
    private var shape: Drawable? = null
    private var drawMatrix: Matrix? = null
    private lateinit var maskMatrix: Matrix

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
        maskMatrix = Matrix()
    }

    fun setShape(
        @DrawableRes shape: Int,
    ) {
        val drawable =
            map[shape].let {
                if (it == null) {
                    val d = AppCompatResources.getDrawable(context, shape)
                    map[shape] = d
                    return@let d
                } else {
                    it
                }
            }
        this.shape = drawable
        super.createMaskCanvas()
    }

    override fun paintMaskCanvas(
        maskCanvas: Canvas,
        maskPaint: Paint,
        width: Int,
        height: Int,
    ) {
        if (shape != null) {
            if (shape is BitmapDrawable) {
                configureBitmapBounds(width, height)
                if (drawMatrix != null) {
                    val drawableSaveCount = maskCanvas.saveCount
                    maskCanvas.save()
                    maskCanvas.concat(maskMatrix)
                    shape!!.draw(maskCanvas)
                    maskCanvas.restoreToCount(drawableSaveCount)
                    return
                }
            }

            shape!!.setBounds(0, 0, width, height)
            shape!!.draw(maskCanvas)
        }
    }

    private fun configureBitmapBounds(
        viewWidth: Int,
        viewHeight: Int,
    ) {
        drawMatrix = null
        val drawableWidth = shape!!.intrinsicWidth
        val drawableHeight = shape!!.intrinsicHeight
        val fits = viewWidth == drawableWidth && viewHeight == drawableHeight

        if (drawableWidth > 0 && drawableHeight > 0 && !fits) {
            shape!!.setBounds(0, 0, drawableWidth, drawableHeight)
            val widthRatio = viewWidth.toFloat() / drawableWidth.toFloat()
            val heightRatio = viewHeight.toFloat() / drawableHeight.toFloat()
            val scale = widthRatio.coerceAtMost(heightRatio)
            val dx = ((viewWidth - drawableWidth * scale) * 0.5f + 0.5f).toInt().toFloat()
            val dy = ((viewHeight - drawableHeight * scale) * 0.5f + 0.5f).toInt().toFloat()

            maskMatrix.setScale(scale, scale)
            maskMatrix.postTranslate(dx, dy)
        }
    }

    companion object {
        private val map = ArrayMap<Int, Drawable>()
    }
}
