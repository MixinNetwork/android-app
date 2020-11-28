package one.mixin.android.ui.common.friends

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import one.mixin.android.databinding.ItemContactNormalBinding
import one.mixin.android.extension.highLight
import one.mixin.android.vo.User

abstract class AbsFriendsAdapter<VH : BaseFriendsViewHolder>(callback: UserItemCallback) : ListAdapter<User, VH>(callback) {
    var listener: FriendsListener? = null
    var filter = ""

    override fun onBindViewHolder(holder: VH, position: Int) {
        getItem(position)?.let {
            holder.bind(it, filter, listener)
        }
    }
}

open class BaseFriendsViewHolder(private val itemBinding: ItemContactNormalBinding) : RecyclerView.ViewHolder(itemBinding.root) {
    open fun bind(item: User, filter: String, listener: FriendsListener?) {
        itemBinding.apply {
            normal.text = item.fullName
            normal.highLight(filter)
            avatar.setInfo(item.fullName, item.avatarUrl, item.userId)
        }
        itemView.setOnClickListener {
            listener?.onItemClick(item)
        }
    }
}

interface FriendsListener {
    fun onItemClick(user: User)
}

class UserItemCallback(var filter: String) : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User) =
        (
            oldItem.fullName?.contains(filter, true) == newItem.fullName?.contains(filter, true) &&
                oldItem.identityNumber.contains(filter, true) == newItem.identityNumber.contains(filter, true)
            )

    override fun areContentsTheSame(oldItem: User, newItem: User) = false
}
