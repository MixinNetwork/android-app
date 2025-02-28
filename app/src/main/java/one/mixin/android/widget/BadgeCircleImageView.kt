package one.mixin.android.widget

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.db.web3.vo.Web3Token
import one.mixin.android.databinding.ViewBadgeCircleImageBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadImage
import one.mixin.android.ui.wallet.alert.vo.CoinItem
import one.mixin.android.vo.market.MarketItem
import one.mixin.android.vo.safe.TokenItem

open class BadgeCircleImageView(context: Context, attrs: AttributeSet?) :
    FrameLayout(context, attrs) {
    private val binding = ViewBadgeCircleImageBinding.inflate(LayoutInflater.from(context), this)
    val bg get() = binding.bg
    val badge get() = binding.badge

    var pos: Int = START_BOTTOM

    val badgeScaleFactor = 3f

    var badgeSize: Int? = null

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.BadgeCircleImageView)
        if (ta.hasValue(R.styleable.BadgeCircleImageView_badge_size)) {
            val badgeSize = ta.getDimensionPixelSize(R.styleable.BadgeCircleImageView_badge_size, -1)
            if (badgeSize > 0) {
                this.badgeSize = badgeSize
            }
        }
        ta.recycle()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        measureChild(
            binding.badge,
            MeasureSpec.makeMeasureSpec(badgeSize?:(measuredWidth / badgeScaleFactor).toInt(), MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(badgeSize?:(measuredHeight / badgeScaleFactor).toInt(), MeasureSpec.EXACTLY),
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
        val badgeWidth = badgeSize ?: (measuredWidth / badgeScaleFactor).toInt()
        if (pos == START_BOTTOM) {
            val positionLeft = (measuredWidth * 0.011f).toInt()
            val positionTop = (measuredHeight - badgeWidth)
            binding.badge.layout(positionLeft, positionTop, positionLeft + badgeWidth, positionTop + badgeWidth)
        } else if (pos == END_BOTTOM) {
            val positionLeft = (measuredWidth - badgeWidth)
            val positionTop = (measuredHeight - badgeWidth)
            binding.badge.layout(positionLeft, positionTop, positionLeft + badgeWidth, positionTop + badgeWidth)
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

    fun loadToken(marketItem: MarketItem) {
        binding.badge.isVisible = false
        binding.bg.loadImage(marketItem.iconUrl, R.drawable.ic_avatar_place_holder)
    }

    fun loadCoin(coinItem: CoinItem) {
        binding.badge.isVisible = false
        binding.bg.loadImage(coinItem.iconUrl, R.drawable.ic_avatar_place_holder)
    }

    fun loadToken(web3Token: Web3TokenItem) {
        binding.bg.loadImage(web3Token.iconUrl, R.drawable.ic_avatar_place_holder)
        binding.badge.isVisible = true
        binding.badge.loadImage(web3Token.chainIcon, R.drawable.ic_avatar_place_holder)
    }

    fun loadToken(
        assetUrl: String,
        chainUrl: String?,
        collectionHash: String?,
    ) {
        if (collectionHash.isNullOrEmpty()) {
            binding.badge.isVisible = true
            binding.bg.loadImage(assetUrl, R.drawable.ic_avatar_place_holder)
            binding.badge.loadImage(chainUrl, R.drawable.ic_avatar_place_holder)
        } else {
            binding.badge.isVisible = false
            binding.bg.loadImage(assetUrl, R.drawable.ic_avatar_place_holder, transformation = CoilRoundedHexagonTransformation())
        }
    }

    companion object {
        const val START_BOTTOM = 0
        const val END_BOTTOM = 1
    }
}
