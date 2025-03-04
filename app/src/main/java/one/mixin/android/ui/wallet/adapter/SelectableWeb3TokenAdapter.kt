package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSelectableTokenBinding
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.MultiSelectWeb3TokenListBottomSheetDialogFragment
import one.mixin.android.vo.Web3TokenDiffCallback

class SelectableWeb3TokenAdapter(private val selectedTokenItems: MutableList<Web3TokenItem>) : ListAdapter<Web3TokenItem, SelectableWeb3TokenAdapter.SearchTokenItemViewHolder>(Web3TokenDiffCallback) {
    var callback: WalletSearchWeb3TokenItemCallback? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SearchTokenItemViewHolder {
        return SearchTokenItemViewHolder(ItemSelectableTokenBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(
        holder: SearchTokenItemViewHolder,
        position: Int,
    ) {
        getItem(position)?.let { holder.bind(it, selectedTokenItems, callback) }
    }

    inner class SearchTokenItemViewHolder(val binding: ItemSelectableTokenBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(
            tokenItem: Web3TokenItem,
            selectedTokenItems: MutableList<Web3TokenItem>,
            tokenItemClickListener: WalletSearchWeb3TokenItemCallback? = null,
        ) {
            binding.name.text = tokenItem.name
            binding.balance.text = "${tokenItem.balance} ${tokenItem.symbol}"
            binding.avatar.loadToken(tokenItem)
            binding.cb.isChecked = selectedTokenItems.contains(tokenItem)
            binding.cb.isClickable = false
            binding.networkTv.isVisible = tokenItem.chainName != null
            if (tokenItem.chainName != null) {
                binding.networkTv.text = tokenItem.getChainDisplayName()
            }
            itemView.setOnClickListener {
                if (!binding.cb.isChecked && selectedTokenItems.size >= MultiSelectWeb3TokenListBottomSheetDialogFragment.LIMIT) {
                    toast(binding.root.context.getString(R.string.Select_LIMIT, MultiSelectWeb3TokenListBottomSheetDialogFragment.LIMIT))
                    return@setOnClickListener
                }
                binding.cb.isChecked = !binding.cb.isChecked
                tokenItemClickListener?.onTokenItemClick(tokenItem)
                notifyItemChanged(adapterPosition)
            }
        }
    }
}

interface WalletSearchWeb3TokenItemCallback {
    fun onTokenItemClick(tokenItem: Web3TokenItem)
}
