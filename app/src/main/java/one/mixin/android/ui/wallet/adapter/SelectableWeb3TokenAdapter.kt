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
import one.mixin.android.db.web3.vo.Web3TokenItem
import one.mixin.android.db.web3.vo.Web3TokenGroup
import one.mixin.android.db.web3.vo.groupWeb3Tokens
import one.mixin.android.db.web3.vo.groupId
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.MultiSelectWeb3TokenListBottomSheetDialogFragment
import one.mixin.android.vo.Web3TokenDiffCallback

class SelectableWeb3TokenAdapter(private val selectedTokenItems: MutableList<Web3TokenItem>, val hide: Boolean = false, private val grouped: Boolean = false) : ListAdapter<Web3TokenItem, SelectableWeb3TokenAdapter.SearchTokenItemViewHolder>(Web3TokenDiffCallback) {
    var callback: WalletSearchWeb3TokenItemCallback? = null
    private var groups: Map<Pair<String, String>, Web3TokenGroup> = emptyMap()

    override fun submitList(list: List<Web3TokenItem>?) = submitList(list, null)

    override fun submitList(list: List<Web3TokenItem>?, commitCallback: Runnable?) {
        groups = if (grouped) list.orEmpty().groupWeb3Tokens().associateBy { it.representative.groupId } else emptyMap()
        super.submitList(if (grouped) groups.values.map { it.representative } else list) {
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
            tokenItem: Web3TokenItem,
            selectedTokenItems: MutableList<Web3TokenItem>,
            tokenItemClickListener: WalletSearchWeb3TokenItemCallback? = null,
        ) {
            if (hide) {
                binding.name.text = tokenItem.symbol
                binding.balance.text = tokenItem.name
            } else {
                binding.name.text = tokenItem.name
                binding.balance.text = "${groups[tokenItem.groupId]?.balance?.toPlainString() ?: tokenItem.balance} ${tokenItem.symbol}"
            }
            binding.avatar.loadToken(tokenItem)
            binding.avatar.badge.isVisible = !grouped
            binding.cb.isChecked = selectedTokenItems.any { if (grouped) it.groupId == tokenItem.groupId else it.walletId == tokenItem.walletId && it.assetId == tokenItem.assetId }
            binding.cb.isClickable = false
            binding.networkTv.isVisible = !grouped && tokenItem.chainName != null
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
