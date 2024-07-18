package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.R
import one.mixin.android.databinding.ItemGroupSelectBinding
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.vo.AddressItem
import one.mixin.android.vo.Recipient
import one.mixin.android.vo.UserItem
import one.mixin.android.vo.displayAddress

class SelectedUserAdapter(val removeUser: (Recipient) -> Unit) : RecyclerView.Adapter<SelectedUserAdapter.SelectViewHolder>() {
    var checkedUsers: List<Recipient>? = null

    class SelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SelectViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_group_select, parent, false)
        return SelectViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return checkedUsers.notNullWithElse({ it.size }, 0)
    }

    override fun onBindViewHolder(
        holder: SelectViewHolder,
        position: Int,
    ) {
        val binding = ItemGroupSelectBinding.bind(holder.itemView)
        checkedUsers?.let { list ->
            val item = list[position]
            if (item is UserItem) {
                binding.avatarView.setInfo(item.fullName, item.avatarUrl, item.id)
                binding.nameTv.text = item.fullName
            } else {
                val address = item as AddressItem
                binding.avatarView.loadUrl(address.iconUrl, R.drawable.ic_avatar_place_holder)
                binding.nameTv.text = address.displayAddress()
            }
        }
        holder.itemView.setOnClickListener {
            checkedUsers?.let { list ->
                val user = list[position]
                removeUser(user)
            }
        }
    }
}
