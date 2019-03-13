package one.mixin.android.ui.conversation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.collection.ArrayMap
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_friend.view.*
import kotlinx.android.synthetic.main.item_search_header.view.*
import one.mixin.android.R
import one.mixin.android.extension.arrayMapOf
import one.mixin.android.extension.inflate
import one.mixin.android.vo.User

class FriendAdapter : RecyclerView.Adapter<FriendAdapter.FriendViewHolder>(),
    StickyRecyclerHeadersAdapter<FriendAdapter.FriendViewHolder> {

    var friends: List<User>? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }
    var listener: FriendListener? = null

    val selectedFriends = arrayMapOf<String, User>()

    override fun getHeaderId(position: Int): Long {
        if (friends.isNullOrEmpty()) return -1
        val u = friends!![position]
        return if (u.fullName != null && u.fullName.isNotEmpty()) u.fullName[0].toLong() else -1L
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup) =
        FriendViewHolder(parent.inflate(R.layout.item_search_header,false))

    override fun onBindHeaderViewHolder(holder: FriendViewHolder, position: Int) {
        if (friends == null || friends!!.isEmpty()) return

        val u = friends!![position]
        holder.itemView.search_header_tv.text = if (u.fullName != null &&
            u.fullName.isNotEmpty()) u.fullName[0].toString() else ""
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        FriendViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_friend,
            parent, false))

    override fun getItemCount() = friends?.size ?: 0

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        if (friends == null || friends!!.isEmpty()) return

        val u = friends!![position]
        holder.bind(u, position, selectedFriends, listener)
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: User, pos: Int, selectedFriends: ArrayMap<String, User>, listener: FriendListener?) {
            itemView.normal.text = item.fullName
            itemView.avatar.setInfo(item.fullName, item.avatarUrl, item.identityNumber)
            itemView.cb.isChecked = selectedFriends[item.userId] != null
            itemView.setOnClickListener {
                if (selectedFriends[item.userId] != null) {
                    selectedFriends.remove(item.userId, item)
                } else {
                    selectedFriends[item.userId] = item
                }
                itemView.cb.isChecked = !itemView.cb.isChecked
                listener?.onFriendClick(pos)
            }
            itemView.bot_iv.visibility = if (item.appId != null) View.VISIBLE else View.GONE
            itemView.verified_iv.visibility = if (item.isVerified == true) View.VISIBLE else View.GONE
        }
    }

    interface FriendListener {
        fun onFriendClick(pos: Int)
    }
}