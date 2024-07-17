package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemGroupFriendBinding
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class SelectableUserAdapter(private val selectedUsers: MutableList<User>) : ListAdapter<User, SelectableUserAdapter.SearchUserViewHolder>(User.DIFF_CALLBACK) {
    var callback: WalletSearchUserCallback? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): SearchUserViewHolder {
        return SearchUserViewHolder(ItemGroupFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(
        holder: SearchUserViewHolder,
        position: Int,
    ) {
        getItem(position)?.let { holder.bind(it, selectedUsers, callback) }
    }

    inner class SearchUserViewHolder(val binding: ItemGroupFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            user: User,
            selectedUsers: MutableList<User>,
            callback: WalletSearchUserCallback? = null,
        ) {
            binding.normal.text = user.fullName
            binding.mixinIdTv.text = user.identityNumber
            binding.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            binding.cb.isChecked = selectedUsers.contains(user)
            user.showVerifiedOrBot(binding.verifiedIv, binding.botIv)
            binding.cb.isClickable = false
            itemView.setOnClickListener {
                binding.cb.isChecked = !binding.cb.isChecked
                callback?.onUserClick(user)
                notifyItemChanged(adapterPosition)
            }
        }
    }
}

interface WalletSearchUserCallback {
    fun onUserClick(user: User)
}

