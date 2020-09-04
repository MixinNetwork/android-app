package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx

open class BadgeCircleImageView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_badge_circle_image, this, true)
    }

    var pos: Int = START_BOTTOM

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChild(
            badge,
            MeasureSpec.makeMeasureSpec(measuredWidth / 4, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight / 4, MeasureSpec.EXACTLY)
        )
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val badgeWidth = measuredWidth / 4
        if (pos == START_BOTTOM) {
            val positionLeft = (measuredWidth * 0.075f).toInt()
            val positionTop = (measuredWidth * 0.725f).toInt()
            badge.layout(positionLeft, positionTop, positionLeft + badgeWidth, positionTop + badgeWidth)
        } else if (pos == END_BOTTOM) {
            val position = (measuredWidth * 0.725f).toInt()
            badge.layout(position, position, position + badgeWidth, position + badgeWidth)
        }
    }

    fun setBorder(width: Float = 2f, color: Int = Color.WHITE) {
        bg.borderWidth = context.dpToPx(width)
        bg.borderColor = color
    }

    companion object {
        const val START_BOTTOM = 0
        const val END_BOTTOM = 1
    }
}
