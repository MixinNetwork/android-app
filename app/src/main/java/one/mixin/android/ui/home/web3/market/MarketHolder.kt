package one.mixin.android.ui.home.web3.market

import android.annotation.SuppressLint
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.databinding.ItemMarketBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.loadSvgWithTint
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.screenWidth
import one.mixin.android.extension.setQuoteText
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.MarketItem
import java.math.BigDecimal

class MarketHolder(val binding: ItemMarketBinding) : RecyclerView.ViewHolder(binding.root) {
    private val horizontalPadding by lazy { binding.root.context.screenWidth() / 20 }
    private val verticalPadding by lazy { 12.dp }

    init {
        applyAdaptiveLayout(binding.root.context.screenWidth())
        binding.root.doOnLayout { applyAdaptiveLayout(it.width) }
        binding.root.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) {
                applyAdaptiveLayout(right - left)
            }
        }
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    fun bind(item: MarketItem, dayType: Boolean, onClick: (MarketItem) -> Unit, onFavorite: (String, String, Boolean?) -> Unit) {
        binding.apply {
            val isColorReversed = binding.root.context.defaultSharedPreferences.getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)
            root.setOnClickListener { onClick.invoke(item) }
            val symbol = Fiats.getSymbol()
            val rate = BigDecimal(Fiats.getRate())
            favorite.setImageResource(if (item.isFavored == true) R.drawable.ic_asset_favorites_checked else R.drawable.ic_asset_favorites)
            favorite.setOnClickListener {
                onFavorite.invoke(item.symbol, item.coinId, item.isFavored)
            }
            icon.loadImage(item.iconUrl, R.drawable.ic_avatar_place_holder)
            assetSymbol.text = item.symbol
            assetValue.text = item.totalVolume
            val percentage = BigDecimal(if (dayType) item.priceChangePercentage7D else item.priceChangePercentage24H)
            marketPercentage.text = "${percentage.numberFormat2()}%"
            val isRising = percentage >= BigDecimal.ZERO
            marketPercentage.setQuoteText("${percentage.numberFormat2()}%", isRising)

            price.text = "$symbol${BigDecimal(item.currentPrice).multiply(rate).priceFormat()}"
            assetNumber.text = item.marketCapRank
            val formatVol = try {
                BigDecimal(item.marketCap).multiply(rate).numberFormatCompact()
            } catch (e: NumberFormatException) {
                null
            }
            assetValue.text = if (formatVol != null) {
                "$symbol$formatVol"
            } else {
                ""
            }
            sparkline.loadSvgWithTint(if (dayType) item.sparklineIn7d else item.sparklineIn24, isRising, isColorReversed)
        }
    }

    private fun applyAdaptiveLayout(width: Int) {
        val isCompact = width in 1 until 380.dp
        val isExtraCompact = width in 1 until 330.dp
        val isTiny = width in 1 until 300.dp
        val rowHorizontalPadding = if (isCompact) 12.dp else horizontalPadding
        val favoriteSize = if (isTiny) 20.dp else 24.dp
        val favoritePadding = if (isTiny) 2.dp else 4.dp
        val iconSize = when {
            isTiny -> 26.dp
            isExtraCompact -> 30.dp
            isCompact -> 34.dp
            else -> 38.dp
        }
        val contentSpacing = when {
            isTiny -> 4.dp
            isExtraCompact -> 6.dp
            isCompact -> 8.dp
            else -> 10.dp
        }
        val nameWidth = when {
            isTiny -> 66.dp
            isExtraCompact -> 72.dp
            isCompact -> 84.dp
            else -> WRAP_CONTENT
        }
        val assetValueMaxWidth = when {
            isTiny -> 48.dp
            isExtraCompact -> 64.dp
            isCompact -> 76.dp
            else -> 280.dp
        }
        val chartWidth = when {
            isTiny -> 42.dp
            isExtraCompact -> 46.dp
            isCompact -> 52.dp
            else -> 60.dp
        }

        binding.container.setPadding(
            (rowHorizontalPadding - 4.dp).coerceAtLeast(8.dp),
            verticalPadding,
            rowHorizontalPadding,
            verticalPadding
        )
        binding.favorite.updateLayoutParams<MarginLayoutParams> {
            this.width = favoriteSize
            this.height = favoriteSize
        }
        binding.favorite.setPadding(favoritePadding, favoritePadding, favoritePadding, favoritePadding)
        binding.icon.updateLayoutParams<MarginLayoutParams> {
            this.width = iconSize
            this.height = iconSize
            marginStart = contentSpacing
        }
        binding.nameLayout.updateLayoutParams<MarginLayoutParams> {
            this.width = nameWidth
            marginStart = contentSpacing
        }
        binding.assetSymbol.maxWidth = nameWidth.takeIf { it != WRAP_CONTENT } ?: Int.MAX_VALUE
        binding.assetValue.maxWidth = assetValueMaxWidth
        binding.price.updateLayoutParams<MarginLayoutParams> {
            marginEnd = if (isCompact) 6.dp else horizontalPadding
        }
        binding.marketRl.updateLayoutParams {
            this.width = chartWidth
        }
        binding.sparkline.updateLayoutParams {
            this.width = chartWidth
        }
    }
}
