package one.mixin.android.ui.common.friends

import android.view.View
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_contact_normal.view.*
import one.mixin.android.extension.highLight
import one.mixin.android.vo.User

abstract class AbsFriendsAdapter<VH : BaseFriendsViewHolder> : ListAdapter<User, VH>(User.DIFF_CALLBACK) {
    var listener: FriendsListener? = null
    var filter = ""

    override fun onBindViewHolder(holder: VH, position: Int) {
        getItem(position)?.let {
            holder.bind(it, filter, listener)
        }
    }
}

open class BaseFriendsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    open fun bind(item: User, filter: String, listener: FriendsListener?) {
        itemView.normal.text = item.fullName
        itemView.normal.highLight(filter)
        itemView.avatar.setInfo(item.fullName, item.avatarUrl, item.userId)
        itemView.setOnClickListener {
            listener?.onItemClick(item)
        }
    }
}

interface FriendsListener {
    fun onItemClick(user: User)
}