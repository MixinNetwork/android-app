package one.mixin.android.ui.wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.databinding.ItemContactHeaderBinding
import one.mixin.android.databinding.ItemSingleSelectFriendBinding
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
        val binding = ItemContactHeaderBinding.bind(holder.itemView)
        if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            binding.header.text = holder.itemView.context.getString(R.string.chat_item_title)
        } else {
            binding.header.text = holder.itemView.context.getString(R.string.contact_item_title)
        }
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        return HeaderViewHolder(ItemContactHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
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
            ConversationViewHolder(ItemSingleSelectFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            FriendViewHolder(ItemSingleSelectFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }
    open class FriendViewHolder(val binding: ItemSingleSelectFriendBinding) : ContactsAdapter.ViewHolder(binding.root) {
        fun bind(user: User, listener: FriendSelectListener?) {
            binding.root.normal.text = user.fullName
            binding.root.avatar.setInfo(user.fullName, user.avatarUrl, user.userId)
            user.showVerifiedOrBot(binding.root.verifiedIv, binding.root.botIv)
            if (listener != null) {
                itemView.setOnClickListener { listener.onItemClick(user) }
            }
        }
    }

    class ConversationViewHolder(binding: ItemSingleSelectFriendBinding) : FriendViewHolder(binding)

    class HeaderViewHolder(val binding: ItemContactHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    interface FriendSelectListener {
        fun onItemClick(user: User)
    }
}
