package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.dpToPx

class ConfirmationBgView : RelativeLayout {
    private val colorWhite by lazy { context.colorFromAttribute(R.attr.bg_white) }
    private val colorConfirmation by lazy { context.colorFromAttribute(R.attr.bg_confirmation) }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var all = 0
    private var cur = 0

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : super(context, attributeSet) {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (all == 0 || cur == 0) {
            paint.color = colorWhite
            canvas.drawRect(0f, 0f, w, h, paint)
        } else {
            val blueWidth = (cur.toFloat() / all) * w
            paint.color = colorWhite
            canvas.drawRect(0f, 0f, w - blueWidth, h, paint)
            paint.color = colorConfirmation
            canvas.drawRect(w - blueWidth, 0f, w, h, paint)
        }
    }

    fun setConfirmation(
        all: Int,
        cur: Int,
    ) {
        this.all = all
        this.cur = cur
    }

    fun roundBottom(round: Boolean) {
        if (round) {
            outlineProvider =
                object : ViewOutlineProvider() {
                    override fun getOutline(
                        view: View,
                        outline: Outline,
                    ) {
                        outline.setRoundRect(
                            0,
                            0,
                            view.width,
                            view.height,
                            context.dpToPx(8f).toFloat(),
                        )
                    }
                }
            clipToOutline = true
        } else {
            clipToOutline = false
        }
    }
}
