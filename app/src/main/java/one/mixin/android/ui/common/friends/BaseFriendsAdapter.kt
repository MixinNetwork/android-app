package one.mixin.android.ui.common.friends

import android.view.View
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
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

abstract class BaseFriendsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: User, filter: String, listener: FriendsListener?)
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
