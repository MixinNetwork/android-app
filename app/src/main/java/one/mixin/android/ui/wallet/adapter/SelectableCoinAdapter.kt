package one.mixin.android.ui.wallet.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSelectableCoinBinding
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.MultiSelectRecipientsListBottomSheetDialogFragment
import one.mixin.android.ui.wallet.alert.vo.CoinItem

class SelectableCoinAdapter(private val selectedCoinItems: MutableList<CoinItem>, private val isSingle:Boolean) : ListAdapter<CoinItem, SelectableCoinAdapter.SearchCoinItemViewHolder>(CoinItem.DIFF_CALLBACK) {
    var callback: WalletSearchCoinItemCallback? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SearchCoinItemViewHolder {
        return SearchCoinItemViewHolder(ItemSelectableCoinBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(
        holder: SearchCoinItemViewHolder,
        position: Int,
    ) {
        getItem(position)?.let { holder.bind(it, selectedCoinItems, callback) }
    }

    inner class SearchCoinItemViewHolder(val binding: ItemSelectableCoinBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(
            coinItem: CoinItem,
            selectedCoinItems: MutableList<CoinItem>,
            coinItemClickListener: WalletSearchCoinItemCallback? = null,
        ) {
            binding.name.text = coinItem.name
            binding.symbol.text = coinItem.symbol
            binding.avatar.loadCoin(coinItem)
            if (!isSingle) {
                binding.cb.isVisible = true
                binding.cb.isChecked = selectedCoinItems.contains(coinItem)
                binding.cb.isClickable = false
            } else {
                binding.cb.isVisible = false
            }
            itemView.setOnClickListener {
                if (!binding.cb.isChecked && selectedCoinItems.size>= MultiSelectRecipientsListBottomSheetDialogFragment.LIMIT) {
                    toast(binding.root.context.getString(R.string.Select_LIMIT, MultiSelectRecipientsListBottomSheetDialogFragment.LIMIT))
                    return@setOnClickListener
                }
                binding.cb.isChecked = !binding.cb.isChecked
                coinItemClickListener?.onCoinItemClick(coinItem)
                notifyItemChanged(adapterPosition)
            }
        }
    }
}

interface WalletSearchCoinItemCallback {
    fun onCoinItemClick(coinItem: CoinItem)
}

