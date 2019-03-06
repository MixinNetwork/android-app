package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import one.mixin.android.R

class BadgeAvatarView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    init {
        LayoutInflater.from(context).inflate(R.layout.view_badge_avatar, this, true)
    }

    var pos: Int = START_BOTTOM

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val badge = findViewById<View>(R.id.badge)
        measureChild(badge, MeasureSpec.makeMeasureSpec(measuredWidth / 4, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight / 4, MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val badge = findViewById<View>(R.id.badge)
        val i = measuredWidth / 8
        when (pos) {
            START_BOTTOM -> badge.layout(0, 5 * i, 2 * i, 7 * i)
            END_BOTTOM -> badge.layout(6 * i, 6 * i, 8 * i, 8 * i)
            END_TOP -> badge.layout(5 * i, i, 7 * i, 3 * i)
        }
    }

    companion object {
        const val START_BOTTOM = 0
        const val END_BOTTOM = 1
        const val END_TOP = 2
    }
}