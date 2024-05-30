package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import coil.load
import one.mixin.android.R
import one.mixin.android.databinding.ViewBadgeCircleImageBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadHexagonImage
import one.mixin.android.vo.safe.TokenItem

open class BadgeCircleImageView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {
    private val binding = ViewBadgeCircleImageBinding.inflate(LayoutInflater.from(context), this)
    val bg get() = binding.bg
    val badge get() = binding.badge

    var pos: Int = START_BOTTOM

    init {
        clipToPadding = false
    }

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

    fun loadToken(tokenItem: TokenItem) {
        loadToken(tokenItem.iconUrl, tokenItem.chainIconUrl, tokenItem.collectionHash)
    }

    fun loadToken(
        assetUrl: String,
        chainUrl: String?,
        collectionHash: String?,
    ) {
        if (collectionHash.isNullOrEmpty()) {
            binding.badge.isVisible = true
            binding.bg.load(assetUrl) {
                placeholder(R.drawable.ic_avatar_place_holder)
            }
            binding.badge.load(chainUrl) {
                placeholder(R.drawable.ic_avatar_place_holder)
            }
        } else {
            binding.badge.isVisible = false
            binding.bg.loadHexagonImage(assetUrl, R.drawable.ic_avatar_place_holder)
        }
    }

    companion object {
        const val START_BOTTOM = 0
        const val END_BOTTOM = 1
    }
}
