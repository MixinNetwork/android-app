package one.mixin.android.ui.conversation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_contact_normal.view.*
import one.mixin.android.R
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class FriendAdapter : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>() {

    var friends: List<User>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var listener: FriendListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FriendViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_contact_normal,
            parent, false))

    override fun getItemCount() = friends?.size ?: 0

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        if (friends == null || friends!!.isEmpty()) return

        val u = friends!![position]
        holder.bind(u, listener)
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: User, listener: FriendListener?) {
            itemView.normal.text = item.fullName
            itemView.avatar.setInfo(item.fullName, item.avatarUrl, item.userId)
            itemView.setOnClickListener {
                listener?.onFriendClick(item)
            }
            item.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)
        }
    }

    interface FriendListener {
        fun onFriendClick(user: User)
    }
}