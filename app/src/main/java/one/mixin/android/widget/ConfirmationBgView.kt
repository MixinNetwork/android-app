package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.widget.RelativeLayout
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dp
import kotlin.math.min

class ConfirmationBgView : RelativeLayout {
    private val colorWhite by lazy { context.colorFromAttribute(R.attr.bg_white) }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var all = 0
    private var cur = 0
    private val dp10 = 10.dp.toFloat()
    private val dp41 = 41.dp.toFloat()


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        setWillNotDraw(false)
    }

    init {
        paint.color = colorWhite
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (all == 0 || cur == 0) {
            paint.shader = null // clear shader
            canvas.drawRect(0f, 0f, w, h, paint)
        } else {
            val gradient = LinearGradient(
                0f, 0f, w, 0f,
                intArrayOf(Color.argb(min(51 * cur / all, 51), 80, 189, 92), Color.argb(51, 80, 189, 92)),
                null,
                Shader.TileMode.CLAMP
            )
            paint.shader = gradient
            canvas.drawRect(dp41, dp10, w, h - dp10, paint)

        }
    }

    fun setConfirmation(
        all: Int,
        cur: Int,
    ) {
        this.all = all
        this.cur = cur
    }
}
