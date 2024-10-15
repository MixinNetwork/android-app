package one.mixin.android.ui.search.holder

import android.annotation.SuppressLint
import android.view.View
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchMarketBinding
import one.mixin.android.extension.highLight
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormat2
import one.mixin.android.extension.priceFormat
import one.mixin.android.extension.setQuoteText
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchExploreFragment
import one.mixin.android.vo.Fiats
import one.mixin.android.vo.market.Market
import java.math.BigDecimal

class MarketHolder(val binding: ItemSearchMarketBinding) : NormalHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun bind(
        market: Market,
        target: String,
        onItemClickListener: SearchExploreFragment.OnSearchClickListener?,
    ) {
        binding.avatar.badge.isVisible = false
        binding.avatar.bg.loadImage(market.iconUrl, R.drawable.ic_avatar_place_holder)
        binding.root.setOnClickListener {
            onItemClickListener?.onMarketClick(market)
        }

        binding.name.text = market.name
        binding.symbol.text = market.symbol
        binding.symbol.highLight(target)
        binding.name.highLight(target)
        if (market.currentPrice == "0") {
            binding.priceTv.setText(R.string.NA)
            binding.changeTv.visibility = View.GONE
        } else {
            binding.changeTv.visibility = View.VISIBLE
            binding.priceTv.text = "${Fiats.getSymbol()}${BigDecimal(market.currentPrice).priceFormat()}"
            if (market.marketCapChangePercentage24h.isNotEmpty()) {
                val changeUsd = BigDecimal(market.marketCapChangePercentage24h)
                val isPositive = changeUsd >= BigDecimal.ZERO
                val t = "${(changeUsd * BigDecimal(100)).numberFormat2()}%"
                binding.changeTv.setQuoteText(if (isPositive) "+$t" else t, isPositive)
            }
        }
    }
}
