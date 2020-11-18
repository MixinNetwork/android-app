package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import one.mixin.android.extension.appCompatActionBarHeight
import kotlin.math.ceil

class SizeNotifierFrameLayout : FrameLayout {

    private val rect = Rect()
    var backgroundImage: Drawable? = null
        set(bitmap) {
            field = bitmap
            invalidate()
        }
    private var keyboardHeight: Int = 0
    private var bottomClip: Int = 0

    constructor(context: Context) : super(context) {
        setWillNotDraw(false)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setWillNotDraw(false)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setWillNotDraw(false)
    }

    init {
        ViewCompat.setOnApplyWindowInsetsListener(this) { _: View?, insets: WindowInsetsCompat ->
            val windowInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )
            updatePadding(top = windowInsets.top, bottom = windowInsets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setBottomClip(value: Int) {
        bottomClip = value
    }

    override fun onDraw(canvas: Canvas) {
        if (backgroundImage != null) {
            if (backgroundImage is ColorDrawable) {
                if (bottomClip != 0) {
                    canvas.save()
                    canvas.clipRect(0, 0, measuredWidth, measuredHeight - bottomClip)
                }
                backgroundImage!!.setBounds(0, 0, measuredWidth, measuredHeight)
                backgroundImage!!.draw(canvas)
                if (bottomClip != 0) {
                    canvas.restore()
                }
            } else if (backgroundImage is BitmapDrawable) {
                val bitmapDrawable = backgroundImage as BitmapDrawable?
                if (bitmapDrawable!!.tileModeX == Shader.TileMode.REPEAT) {
                    canvas.save()
                    val scale: Float = 2.0f / context.resources.displayMetrics.density
                    canvas.scale(scale, scale)
                    backgroundImage!!.setBounds(
                        0,
                        0,
                        ceil((measuredWidth / scale).toDouble()).toInt(),
                        ceil((measuredHeight / scale).toDouble()).toInt()
                    )
                    backgroundImage!!.draw(canvas)
                    canvas.restore()
                } else {
                    val actionBarHeight = context.appCompatActionBarHeight()
                    val viewHeight = measuredHeight - actionBarHeight
                    val scaleX =
                        measuredWidth.toFloat() / backgroundImage!!.intrinsicWidth.toFloat()
                    val scaleY =
                        (viewHeight + keyboardHeight).toFloat() / backgroundImage!!.intrinsicHeight.toFloat()
                    val scale = if (scaleX < scaleY) scaleY else scaleX
                    val width = ceil((backgroundImage!!.intrinsicWidth * scale).toDouble()).toInt()
                    val height =
                        ceil((backgroundImage!!.intrinsicHeight * scale).toDouble()).toInt()
                    val x = (measuredWidth - width) / 2
                    val y = (viewHeight - height + keyboardHeight) / 2 + actionBarHeight
                    if (bottomClip != 0) {
                        canvas.save()
                        canvas.clipRect(0, actionBarHeight, width, measuredHeight)
                    }
                    backgroundImage!!.setBounds(x, y, x + width, y + height)
                    backgroundImage!!.draw(canvas)
                    if (bottomClip != 0) {
                        canvas.restore()
                    }
                }
            }
        } else {
            super.onDraw(canvas)
        }
    }
}
