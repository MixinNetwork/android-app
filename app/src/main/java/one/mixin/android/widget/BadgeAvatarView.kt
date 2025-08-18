package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import one.mixin.android.databinding.ViewBadgeAvatarBinding

class BadgeAvatarView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val binding = ViewBadgeAvatarBinding.inflate(LayoutInflater.from(context), this)
    val bg get() = binding.bg
    val badge get() = binding.badge

    var pos: Int = START_BOTTOM

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChild(
            binding.badge,
            MeasureSpec.makeMeasureSpec(measuredWidth / 4, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight / 4, MeasureSpec.EXACTLY),
        )
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        super.onLayout(changed, left, top, right, bottom)
        val i = measuredWidth / 8
        if (pos == START_BOTTOM) {
            binding.badge.layout(0, 5 * i, 2 * i, 7 * i)
        } else if (pos == END_BOTTOM) {
            binding.badge.layout(6 * i, 6 * i, 8 * i, 8 * i)
        } else if (pos == END_TOP) {
            binding.badge.layout(6 * i, 0, 8 * i, 2 * i)
        }
    }

    companion object {
        const val START_BOTTOM = 0
        const val END_BOTTOM = 1
        const val END_TOP = 2
    }
}
