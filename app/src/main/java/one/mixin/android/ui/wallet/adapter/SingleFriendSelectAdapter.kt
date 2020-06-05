package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import kotlinx.android.synthetic.main.item_contact_header.view.*
import kotlinx.android.synthetic.main.view_conversation_check.view.*
import one.mixin.android.R
import one.mixin.android.extension.inflate
import one.mixin.android.ui.contacts.ContactsAdapter
import one.mixin.android.vo.User
import one.mixin.android.vo.showVerifiedOrBot

class SingleFriendSelectAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickyRecyclerHeadersAdapter<SingleFriendSelectAdapter.HeaderViewHolder> {

    companion object {
        const val TYPE_CONVERSATION = 0
        const val TYPE_FRIEND = 1
    }

    var listener: FriendSelectListener? = null
    var conversations: List<User>? = null
    var friends: List<User>? = null
    var showHeader: Boolean = true

    override fun getItemCount(): Int {
        return if (conversations == null && friends == null) {
            0
        } else if (conversations == null) {
            friends?.size ?: 0
        } else if (friends == null) {
            conversations?.size ?: 0
        } else {
            conversations!!.size + friends!!.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            TYPE_CONVERSATION
        } else {
            TYPE_FRIEND
        }
    }

    override fun getHeaderId(position: Int): Long {
        if (!showHeader) {
            return -1
        }
        return if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            1
        } else {
            2
        }
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        if (conversations == null || conversations!!.isEmpty() && friends == null && friends!!.isEmpty()) {
            return
        }
        if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            holder.itemView.header.text = holder.itemView.context.getString(R.string.chat_item_title)
        } else {
            holder.itemView.header.text = holder.itemView.context.getString(R.string.contact_item_title)
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        val view = parent.inflate(R.layout.item_contact_header, false)
        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (conversations == null || conversations!!.isEmpty() && friends == null && friends!!.isEmpty()) {
            return
        }
        if (holder is ConversationViewHolder) {
            val user = conversations!![position]
            holder.bind(user, listener)
        } else {
            holder as FriendViewHolder
            val pos = if (conversations != null && conversations!!.isNotEmpty())
                position - conversations!!.size else position
            val user = friends!![pos]
            holder.bind(user, listener)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CONVERSATION) {
            ConversationViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_single_select_friend, parent, false))
        } else {
            FriendViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_single_select_friend, parent, false))
        }
    }

    open class FriendViewHolder(itemView: View) : ContactsAdapter.ViewHolder(itemView) {
        fun bind(user: User, listener: FriendSelectListener?) {
            itemView.normal.text = user.fullName
            itemView.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            user.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)
            if (listener != null) {
                itemView.setOnClickListener { listener.onItemClick(user) }
            }
        }
    }

    class ConversationViewHolder(itemView: View) : FriendViewHolder(itemView)

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    interface FriendSelectListener {
        fun onItemClick(user: User)
    }
}
