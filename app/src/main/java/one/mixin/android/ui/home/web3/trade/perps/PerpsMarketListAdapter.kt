package one.mixin.android.ui.home.web3.trade.perps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
) : RecyclerView.Adapter<PerpsMarketListAdapter.MarketViewHolder>() {

    private var markets = listOf<PerpsMarket>()

    fun submitList(list: List<PerpsMarket>) {
        markets = list
        notifyDataSetChanged()
    }

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
        holder.bind(markets[position])
    }

    override fun getItemCount() = markets.size

    inner class MarketViewHolder(
        private val binding: ItemMarketListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(market: PerpsMarket) {
            binding.apply {
                val fiatRate = BigDecimal(Fiats.getRate())
                val fiatSymbol = Fiats.getSymbol()
                iconIv.loadImage(market.iconUrl, R.drawable.ic_avatar_place_holder)
                symbolTv.text = market.displaySymbol

                val formattedVolume = try {
                    BigDecimal(market.volume).multiply(fiatRate).numberFormatCompact()
                } catch (e: Exception) {
                    market.volume
                }
                volumeTv.text = root.context.getString(R.string.Vol, "$fiatSymbol$formattedVolume")

                val formattedPrice = try {
                    BigDecimal(market.markPrice).multiply(fiatRate).priceFormat()
                } catch (e: Exception) {
                    market.markPrice
                }
                priceTv.text = "$fiatSymbol$formattedPrice"

                val change = try {
                    BigDecimal(market.change)
                } catch (e: Exception) {
                    BigDecimal.ZERO
                }

                val isPositive = change >= BigDecimal.ZERO
                val changeColor = ContextCompat.getColor(
                    root.context,
                    if (isPositive) {
                        if (isQuoteColorReversed) R.color.wallet_red else R.color.wallet_green
                    } else {
                        if (isQuoteColorReversed) R.color.wallet_green else R.color.wallet_red
                    }
                )
                val changeText = "${if (isPositive) "+" else ""}${market.change}%"
                
                changeTv.text = changeText
                changeTv.setTextColor(changeColor)

                root.setOnClickListener {
                    onMarketClick(market)
                }
            }
        }
    }
}
