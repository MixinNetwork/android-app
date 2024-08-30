package one.mixin.android.ui.home.web3.market

import androidx.recyclerview.widget.DiffUtil
import one.mixin.android.vo.market.MarketItem

class MarketDiffCallback(
        private val oldList: List<MarketItem>,
        private val newList: List<MarketItem>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size

        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].coinId == newList[newItemPosition].coinId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
