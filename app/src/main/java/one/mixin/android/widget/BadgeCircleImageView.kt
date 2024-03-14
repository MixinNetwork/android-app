package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import one.mixin.android.databinding.ViewBadgeCircleImageBinding
import one.mixin.android.extension.dpToPx

open class BadgeCircleImageView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {
    private val binding = ViewBadgeCircleImageBinding.inflate(LayoutInflater.from(context), this)
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
            MeasureSpec.makeMeasureSpec(measuredWidth / 3, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(measuredHeight / 3, MeasureSpec.EXACTLY),
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
        val badgeWidth = measuredWidth / 3
        if (pos == START_BOTTOM) {
            val positionLeft = (measuredWidth * 0.011f).toInt()
            val positionTop = (measuredWidth * 0.7f).toInt()
            binding.badge.layout(positionLeft, positionTop, positionLeft + badgeWidth, positionTop + badgeWidth)
        } else if (pos == END_BOTTOM) {
            val position = (measuredWidth * 0.7f).toInt()
            binding.badge.layout(position, position, position + badgeWidth, position + badgeWidth)
        }
    }

    fun setBorder(
        width: Float = 2f,
        color: Int = Color.WHITE,
    ) {
        binding.bg.borderWidth = context.dpToPx(width)
        binding.bg.borderColor = color
    }

    companion object {
        const val START_BOTTOM = 0
        const val END_BOTTOM = 1
    }
}
