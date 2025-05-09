package one.mixin.android.ui.home.web3.market

import androidx.recyclerview.widget.DiffUtil
import one.mixin.android.vo.market.MarketItem

class MarketDiffCallback : DiffUtil.ItemCallback<MarketItem>() {
    override fun areItemsTheSame(oldItem: MarketItem, newItem: MarketItem): Boolean {
        return oldItem.coinId == newItem.coinId
    }

    override fun areContentsTheSame(oldItem: MarketItem, newItem: MarketItem): Boolean {
        return oldItem == newItem
    }
}