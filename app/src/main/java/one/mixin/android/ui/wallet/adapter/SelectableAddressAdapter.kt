package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemSelectableAddressBinding
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.toast
import one.mixin.android.ui.wallet.MultiSelectRecipientsListBottomSheetDialogFragment
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.Recipient
import one.mixin.android.vo.displayAddress

class SelectableAddressAdapter(private val selectedUsers: MutableList<Recipient>) : ListAdapter<AddressItem, SelectableAddressAdapter.SearchAddressViewHolder>(AddressItem.DIFF_CALLBACK) {
    var callback: WalletSearchAddressCallback? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SearchAddressViewHolder {
        return SearchAddressViewHolder(ItemSelectableAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(
        holder: SearchAddressViewHolder,
        position: Int,
    ) {
        getItem(position)?.let { holder.bind(it, selectedUsers, callback) }
    }

    inner class SearchAddressViewHolder(val binding: ItemSelectableAddressBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            address: AddressItem,
            selectedRecipients: MutableList<Recipient>,
            callback: WalletSearchAddressCallback? = null,
        ) {
            binding.label.text = address.label
            binding.addressTv.text = address.displayAddress()
            binding.avatar.bg.loadImage(address.chainUrl, R.drawable.ic_avatar_place_holder)
            binding.avatar.badge.isVisible = false
            binding.cb.isChecked = selectedRecipients.contains(address)
            binding.cb.isClickable = false
            itemView.setOnClickListener {
                if (!binding.cb.isChecked && selectedRecipients.size >= MultiSelectRecipientsListBottomSheetDialogFragment.LIMIT) {
                    toast(binding.root.context.getString(R.string.Select_LIMIT, MultiSelectRecipientsListBottomSheetDialogFragment.LIMIT))
                    return@setOnClickListener
                }
                binding.cb.isChecked = !binding.cb.isChecked
                callback?.onAddressClick(address)
                notifyItemChanged(adapterPosition)
            }
        }
    }
}

interface WalletSearchAddressCallback {
    fun onAddressClick(address: AddressItem)
}

