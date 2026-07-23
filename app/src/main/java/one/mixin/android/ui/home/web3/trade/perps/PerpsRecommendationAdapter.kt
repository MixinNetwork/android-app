package one.mixin.android.ui.home.web3.trade.perps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.api.response.perps.PerpsMarket
import one.mixin.android.databinding.ItemPerpsWatchlistRecommendationBinding
import one.mixin.android.extension.loadImage
import java.math.BigDecimal

class PerpsRecommendationAdapter(
    private val isQuoteColorReversed: Boolean,
    private val onSelectionChanged: (PerpsMarket, Boolean) -> Unit,
) : ListAdapter<PerpsMarket, PerpsRecommendationAdapter.RecommendationViewHolder>(DiffCallback()) {
    var selectedMarketIds: Set<String> = emptySet()
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecommendationViewHolder =
        RecommendationViewHolder(
            ItemPerpsWatchlistRecommendationBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )

    override fun onBindViewHolder(holder: RecommendationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecommendationViewHolder(
        private val binding: ItemPerpsWatchlistRecommendationBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(market: PerpsMarket) {
            val isSelected = market.marketId in selectedMarketIds
            binding.apply {
                iconIv.loadImage(market.iconUrl, R.drawable.ic_avatar_place_holder)
                symbolTv.text = market.tokenSymbol
                leverageTv.text = root.context.getString(R.string.Perpetual_Leverage_Format, market.leverage)
                val change = market.changePercent()
                changeTv.text = formatPerpsSignedPercent(change)
                changeTv.setTextColor(
                    ContextCompat.getColor(
                        root.context,
                        if (change >= BigDecimal.ZERO) {
                            if (isQuoteColorReversed) R.color.wallet_red else R.color.wallet_green
                        } else {
                            if (isQuoteColorReversed) R.color.wallet_green else R.color.wallet_red
                        },
                    ),
                )
                selectedIv.setImageResource(
                    if (isSelected) {
                        R.drawable.ic_asset_favorites_checked
                    } else {
                        R.drawable.ic_asset_favorites
                    },
                )
                root.setOnClickListener {
                    onSelectionChanged(market, !isSelected)
                }
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<PerpsMarket>() {
        override fun areItemsTheSame(oldItem: PerpsMarket, newItem: PerpsMarket): Boolean =
            oldItem.marketId == newItem.marketId

        override fun areContentsTheSame(oldItem: PerpsMarket, newItem: PerpsMarket): Boolean =
            oldItem == newItem
    }
}
