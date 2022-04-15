package one.mixin.android.ui.forward

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import one.mixin.android.R
import one.mixin.android.databinding.ItemContactHeaderBinding
import one.mixin.android.extension.containsIgnoreCase
import one.mixin.android.extension.equalsIgnoreCase
import one.mixin.android.extension.startsWithIgnoreCase
import one.mixin.android.vo.ConversationMinimal
import one.mixin.android.vo.User
import one.mixin.android.vo.isGroupConversation
import one.mixin.android.widget.ConversationCheckView

class ForwardAdapter(private val disableCheck: Boolean = false) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickyRecyclerHeadersAdapter<ForwardAdapter.HeaderViewHolder> {

    companion object {
        const val TYPE_CONVERSATION = 0
        const val TYPE_FRIEND = 1
        const val TYPE_BOT = 2
    }

    var selectItem = ArrayList<Any>()

    private var listener: ForwardListener? = null
    var conversations: List<ConversationMinimal>? = null
    var friends: List<User>? = null
    var bots: List<User>? = null

    var sourceConversations: List<ConversationMinimal>? = null
    var sourceFriends: List<User>? = null
    var sourceBots: List<User>? = null

    var showHeader: Boolean = true
    var keyword: CharSequence? = null

    @SuppressLint("NotifyDataSetChanged")
    fun changeData() {
        if (!keyword.isNullOrBlank()) {
            conversations = sourceConversations?.filter {
                if (it.isGroupConversation()) {
                    it.groupName != null && (it.groupName.containsIgnoreCase(keyword))
                } else {
                    it.name.containsIgnoreCase(keyword) ||
                        it.ownerIdentityNumber.startsWithIgnoreCase(keyword)
                }
            }?.sortedByDescending {
                if (it.isGroupConversation()) {
                    it.groupName.equalsIgnoreCase(keyword)
                } else {
                    it.name.equalsIgnoreCase(keyword) || it.ownerIdentityNumber.equalsIgnoreCase(keyword)
                }
            }
            friends = sourceFriends?.filter {
                (it.fullName != null && it.fullName.containsIgnoreCase(keyword)) ||
                    it.identityNumber.startsWithIgnoreCase(keyword)
            }?.sortedByDescending {
                it.fullName.equalsIgnoreCase(keyword) || it.identityNumber.equalsIgnoreCase(keyword)
            }
            bots = sourceBots?.filter {
                (it.fullName != null && it.fullName.containsIgnoreCase(keyword)) ||
                    it.identityNumber.startsWithIgnoreCase(keyword)
            }?.sortedByDescending {
                it.fullName.equalsIgnoreCase(keyword) || it.identityNumber.equalsIgnoreCase(keyword)
            }
            showHeader = false
        } else {
            conversations = sourceConversations
            friends = sourceFriends
            bots = sourceBots
            showHeader = true
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return if (conversations == null && friends == null && bots == null) {
            0
        } else if (conversations == null) {
            friends?.size?.plus(bots?.size ?: 0) ?: 0
        } else if (friends == null) {
            conversations?.size?.plus(bots?.size ?: 0) ?: 0
        } else if (bots == null) {
            friends?.size?.plus(conversations?.size ?: 0) ?: 0
        } else {
            conversations!!.size + friends!!.size + bots!!.size
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            TYPE_CONVERSATION
        } else if (friends != null && friends!!.isNotEmpty() && position < conversations!!.size + friends!!.size) {
            TYPE_FRIEND
        } else {
            TYPE_BOT
        }
    }

    override fun getHeaderId(position: Int): Long {
        if (!showHeader) {
            return -1
        }
        return if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
            1
        } else if (friends != null && friends!!.isNotEmpty() && position < conversations!!.size + friends!!.size) {
            2
        } else {
            3
        }
    }

    override fun onBindHeaderViewHolder(holder: HeaderViewHolder, position: Int) {
        if (conversations.isNullOrEmpty() && friends.isNullOrEmpty() && bots.isNullOrEmpty()) {
            return
        }
        ItemContactHeaderBinding.bind(holder.itemView).header.text = holder.itemView.context.getString(
            if (conversations != null && conversations!!.isNotEmpty() && position < conversations!!.size) {
                R.string.chat_capital_item_title
            } else if (friends != null && friends!!.isNotEmpty() && position < conversations!!.size + friends!!.size) {
                R.string.CONTACTS
            } else {
                R.string.Bots
            }
        )
    }

    override fun onCreateHeaderViewHolder(parent: ViewGroup): HeaderViewHolder {
        return HeaderViewHolder(ItemContactHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (conversations.isNullOrEmpty() && friends.isNullOrEmpty() && bots.isNullOrEmpty()) {
            return
        }
        when (holder) {
            is ConversationViewHolder -> {
                val conversationItem = conversations!![position]
                holder.bind(conversationItem, listener, selectItem.contains(conversationItem))
            }
            is FriendViewHolder -> {
                val pos = position - (conversations?.size ?: 0)
                val user = friends!![pos]
                holder.bind(user, listener, selectItem.contains(user))
            }
            else -> {
                holder as BotViewHolder
                val pos = position - (conversations?.size ?: 0) - (friends?.size ?: 0)
                val user = bots!![pos]
                holder.bind(user, listener, selectItem.contains(user))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_CONVERSATION -> {
                ConversationViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_forward_conversation,
                        parent,
                        false
                    ).apply {
                        if (disableCheck) {
                            (this as ConversationCheckView).disableCheck()
                        }
                    }
                )
            }
            TYPE_FRIEND -> {
                FriendViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_contact_friend,
                        parent,
                        false
                    ).apply {
                        if (disableCheck) {
                            (this as ConversationCheckView).disableCheck()
                        }
                    }
                )
            }
            else -> {
                BotViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                        R.layout.item_contact_friend,
                        parent,
                        false
                    ).apply {
                        if (disableCheck) {
                            (this as ConversationCheckView).disableCheck()
                        }
                    }
                )
            }
        }
    }

    fun setForwardListener(listener: ForwardListener) {
        this.listener = listener
    }

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: User, listener: ForwardListener?, isCheck: Boolean) {
            (itemView as ConversationCheckView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: User, listener: ForwardListener?, isCheck: Boolean) {
            (itemView as ConversationCheckView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: ConversationMinimal, listener: ForwardListener?, isCheck: Boolean) {
            (itemView as ConversationCheckView).let {
                it.isChecked = isCheck
                it.bind(item, listener)
            }
        }
    }

    class HeaderViewHolder(binding: ItemContactHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    interface ForwardListener {
        fun onUserItemClick(user: User)
        fun onConversationClick(item: ConversationMinimal)
    }
}
