package one.mixin.android.ui.home.web3.trade.perps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.databinding.ItemMarketListBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.numberFormatCompact
import one.mixin.android.extension.priceFormat
import one.mixin.android.vo.Fiats
import java.math.BigDecimal

class PerpsMarketListAdapter(
    private val isQuoteColorReversed: Boolean,
    private val onMarketClick: (PerpsMarket) -> Unit
) : ListAdapter<PerpsMarket, PerpsMarketListAdapter.MarketViewHolder>(PerpsMarketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketViewHolder {
        return MarketViewHolder(
            ItemMarketListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MarketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MarketViewHolder(
        private val binding: ItemMarketListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(market: PerpsMarket) {
            binding.apply {
                val fiatRate = BigDecimal(Fiats.getRate())
                val fiatSymbol = Fiats.getSymbol()
                if (iconIv.tag != market.iconUrl) {
                    iconIv.tag = market.iconUrl
                    iconIv.loadImage(market.iconUrl, R.drawable.ic_avatar_place_holder)
                }
                symbolTv.text = market.tokenSymbol
                leverageTv.text = root.context.getString(R.string.Perpetual_Leverage_Format, market.leverage)

                val formattedVolume = try {
                    BigDecimal(market.volume).multiply(fiatRate).numberFormatCompact()
                } catch (e: Exception) {
                    market.volume
                }
                volumeTv.text = root.context.getString(R.string.Vol, "$fiatSymbol$formattedVolume")

                val formattedPrice = try {
                    BigDecimal(market.last).priceFormat()
                } catch (e: Exception) {
                    market.last
                }
                priceTv.text = "$formattedPrice"

                val changePercent = market.changePercent()
                val isPositive = changePercent >= BigDecimal.ZERO
                val changeColor = ContextCompat.getColor(
                    root.context,
                    if (isPositive) {
                        if (isQuoteColorReversed) R.color.wallet_red else R.color.wallet_green
                    } else {
                        if (isQuoteColorReversed) R.color.wallet_green else R.color.wallet_red
                    }
                )
                val changeText = formatPerpsSignedPercent(changePercent)

                changeTv.text = changeText
                changeTv.setTextColor(changeColor)

                root.setOnClickListener {
                    onMarketClick(market)
                }
            }
        }
    }

    private class PerpsMarketDiffCallback : DiffUtil.ItemCallback<PerpsMarket>() {
        override fun areItemsTheSame(oldItem: PerpsMarket, newItem: PerpsMarket): Boolean {
            return oldItem.marketId == newItem.marketId
        }

        override fun areContentsTheSame(oldItem: PerpsMarket, newItem: PerpsMarket): Boolean {
            return oldItem == newItem
        }
    }
}
