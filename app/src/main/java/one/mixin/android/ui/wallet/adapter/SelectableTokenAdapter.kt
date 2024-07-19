package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSelectableTokenBinding
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.MultiSelectRecipientsListBottomSheetDialogFragment
import one.mixin.android.util.getChainNetwork
import one.mixin.android.vo.safe.TokenItem

class SelectableTokenAdapter(private val selectedTokenItems: MutableList<TokenItem>) : ListAdapter<TokenItem, SelectableTokenAdapter.SearchTokenItemViewHolder>(TokenItem.DIFF_CALLBACK) {
    var callback: WalletSearchTokenItemCallback? = null

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
            tokenItem: TokenItem,
            selectedTokenItems: MutableList<TokenItem>,
            tokenItemClickListener: WalletSearchTokenItemCallback? = null,
        ) {
            binding.name.text = tokenItem.name
            binding.balance.text = "${tokenItem.balance} ${tokenItem.symbol}"
            binding.avatar.loadToken(tokenItem)
            binding.cb.isChecked = selectedTokenItems.contains(tokenItem)
            binding.cb.isClickable = false
            val chainNetwork = getChainNetwork(tokenItem.assetId, tokenItem.chainId, tokenItem.assetKey)
            binding.networkTv.isVisible = chainNetwork != null && tokenItem.collectionHash.isNullOrEmpty()
            if (chainNetwork != null) {
                binding.networkTv.text = chainNetwork
            }
            itemView.setOnClickListener {
                if (!binding.cb.isChecked && selectedTokenItems.size>= MultiSelectRecipientsListBottomSheetDialogFragment.LIMIT) {
                    toast(binding.root.context.getString(R.string.Select_LIMIT, MultiSelectRecipientsListBottomSheetDialogFragment.LIMIT))
                    return@setOnClickListener
                }
                binding.cb.isChecked = !binding.cb.isChecked
                tokenItemClickListener?.onTokenItemClick(tokenItem)
                notifyItemChanged(adapterPosition)
            }
        }
    }
}

interface WalletSearchTokenItemCallback {
    fun onTokenItemClick(tokenItem: TokenItem)
}

