@file:Suppress("DEPRECATION")

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
import one.mixin.android.vo.safe.TokenGroup
import one.mixin.android.vo.safe.groupTokens
import one.mixin.android.vo.safe.groupId

class SelectableTokenAdapter(private val selectedTokenItems: MutableList<TokenItem>) : ListAdapter<TokenItem, SelectableTokenAdapter.SearchTokenItemViewHolder>(TokenItem.DIFF_CALLBACK) {
    var callback: WalletSearchTokenItemCallback? = null
    private var groups: Map<String, TokenGroup> = emptyMap()

    override fun submitList(list: List<TokenItem>?) = submitList(list, null)

    override fun submitList(list: List<TokenItem>?, commitCallback: Runnable?) {
        groups = list.orEmpty().groupTokens().associateBy { it.representative.assetId }
        super.submitList(groups.values.map { it.representative }) {
            notifyItemRangeChanged(0, itemCount)
            commitCallback?.run()
        }
    }

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
            binding.balance.text = "${groups[tokenItem.assetId]?.balance?.toPlainString() ?: tokenItem.balance} ${tokenItem.symbol}"
            binding.avatar.loadToken(tokenItem)
            binding.avatar.badge.isVisible = false
            binding.cb.isChecked = selectedTokenItems.any { it.groupId == tokenItem.groupId }
            binding.cb.isClickable = false
            val chainNetwork = getChainNetwork(tokenItem.assetId, tokenItem.chainId, tokenItem.assetKey)
            binding.networkTv.isVisible = false
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

