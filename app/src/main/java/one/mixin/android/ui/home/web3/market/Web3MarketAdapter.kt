package one.mixin.android.ui.home.web3.market


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemMarketBinding
import one.mixin.android.vo.market.MarketItem

class Web3MarketAdapter(private val sourceRank: Boolean, private val onClick: (MarketItem) -> Unit, private val onFavorite: (String, Boolean?) -> Unit) : RecyclerView.Adapter<MarketHolder>() {
        var items: List<MarketItem> = emptyList()
            set(value) {
                val diffResult = DiffUtil.calculateDiff(MarketDiffCallback(field, value))
                field = value
                diffResult.dispatchUpdatesTo(this)
            }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketHolder {
            return MarketHolder(ItemMarketBinding.inflate(LayoutInflater.from(parent.context)))
        }

        override fun onBindViewHolder(holder: MarketHolder, position: Int) {
            holder.bind(items[position], sourceRank, onClick, onFavorite)
        }

        override fun getItemCount(): Int = items.size
    }