package one.mixin.android.vo

import androidx.recyclerview.widget.DiffUtil
import one.mixin.android.db.web3.vo.Web3TokenItem

object Web3TokenDiffCallback : DiffUtil.ItemCallback<Web3TokenItem>() {
    override fun areItemsTheSame(oldItem: Web3TokenItem, newItem: Web3TokenItem): Boolean {
        return oldItem.assetId == newItem.assetId
    }

    override fun areContentsTheSame(oldItem: Web3TokenItem, newItem: Web3TokenItem): Boolean {
        return oldItem == newItem
    }
}
