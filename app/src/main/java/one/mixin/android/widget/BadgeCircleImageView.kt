package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.view_badge_circle_image.view.*
import one.mixin.android.R

class BadgeCircleImageView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_badge_circle_image, this, true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChild(badge, MeasureSpec.makeMeasureSpec(measuredWidth / 4, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight / 4, MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val i = measuredWidth / 8
        badge.layout(0, 5 * i, 2 * i, 7 * i)
    }
}