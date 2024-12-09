package one.mixin.android.ui.home.web3.market

import android.annotation.SuppressLint
import android.view.ViewGroup.MarginLayoutParams
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
        binding.container.setPadding(horizontalPadding - 4.dp, verticalPadding, horizontalPadding, verticalPadding)
        binding.price.updateLayoutParams<MarginLayoutParams> {
            marginEnd = horizontalPadding
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
            sparkline.loadSvgWithTint(if (dayType) item.sparklineIn7d else item.sparklineIn7d, isRising, isColorReversed)
        }
    }
}