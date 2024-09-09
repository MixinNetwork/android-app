package one.mixin.android.ui.home.web3.market

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import one.mixin.android.databinding.ItemMarketBinding
import one.mixin.android.vo.market.MarketItem

class Web3MarketAdapter(private val sourceRank: Boolean, private val onClick: (MarketItem) -> Unit, private val onFavorite: (String, String, Boolean?) -> Unit) : PagingDataAdapter<MarketItem,MarketHolder>(MarketDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketHolder {
        return MarketHolder(ItemMarketBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: MarketHolder, position: Int) {
        getItem(position)?.let {
            holder.bind(it, onClick, onFavorite)
        }
    }
}