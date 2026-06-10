package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.extension.appCompatActionBarHeight
import kotlin.math.ceil

class BackgroundConstraintLayout : ConstraintLayout {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    init {
        setWillNotDraw(false)
    }

    var backgroundImage: Drawable? = null
        set(bitmap) {
            field = bitmap
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        val bg = backgroundImage
        if (bg != null) {
            val actionBarHeight = context.appCompatActionBarHeight()
            val scaleX = measuredWidth.toFloat() / bg.intrinsicWidth.toFloat()
            val scaleY = (measuredHeight).toFloat() / bg.intrinsicHeight.toFloat()
            val scale = if (scaleX < scaleY) scaleY else scaleX
            val width = ceil((bg.intrinsicWidth * scale).toDouble()).toInt()
            val height =
                ceil((bg.intrinsicHeight * scale).toDouble()).toInt()
            val x = (measuredWidth - width) / 2
            val y = (measuredHeight - height) / 2
            canvas.save()
            canvas.clipRect(
                0,
                actionBarHeight,
                measuredWidth,
                measuredHeight,
            )
            bg.setBounds(x, y, x + width, y + height)
            bg.draw(canvas)
            canvas.restore()
        } else {
            super.onDraw(canvas)
        }
    }
}
