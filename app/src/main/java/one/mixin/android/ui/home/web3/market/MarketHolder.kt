package one.mixin.android.ui.home.web3.market

import android.annotation.SuppressLint
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemMarketBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.loadSvgWithRedTint
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.screenWidth
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
    fun bind(item: MarketItem, sourceRank: Boolean, onClick: (MarketItem) -> Unit, onFavorite: (String, Boolean?) -> Unit) {
        binding.apply {
            root.setOnClickListener { onClick.invoke(item) }
            val symbol = Fiats.getSymbol()
            val rate = BigDecimal(Fiats.getRate())
            favorite.setImageResource(if (item.isFavored == true) R.drawable.ic_market_favorites_checked else R.drawable.ic_market_favorites)
            favorite.setOnClickListener {
                onFavorite.invoke(item.coinId, item.isFavored)
            }
            icon.loadImage(item.iconUrl, R.drawable.ic_avatar_place_holder)
            assetSymbol.text = item.symbol
            assetValue.text = item.totalVolume
            price.text = "$symbol${BigDecimal(item.currentPrice).multiply(rate).priceFormat()}"
            assetNumber.text = if (sourceRank) item.marketCapRank else "${absoluteAdapterPosition + 1}"
            val formatVol = try {
                BigDecimal(item.totalVolume).multiply(rate).numberFormatCompact()
            } catch (e: NumberFormatException) {
                null
            }
            assetValue.text = if (formatVol != null) {
                "$symbol$formatVol"
            } else {
                ""
            }
            market.loadSvgWithRedTint(
                item.sparklineIn7d, !item.priceChangePercentage7D
                    .startsWith("-"), false
            )
        }
    }
}