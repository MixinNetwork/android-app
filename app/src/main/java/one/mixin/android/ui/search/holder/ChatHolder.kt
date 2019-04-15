package one.mixin.android.ui.search.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_search_contact.view.*
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationCategory

class ChatHolder constructor(containerView: View) : RecyclerView.ViewHolder(containerView) {
    init {
        itemView.bot_iv.visibility = View.GONE
        itemView.verified_iv.visibility = View.GONE
    }

    fun bind(chat: ChatMinimal, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        if (chat.category == ConversationCategory.CONTACT.name) {
            itemView.search_name.text = chat.fullName
            itemView.search_avatar_iv.setInfo(chat.fullName, chat.avatarUrl, chat.userId)
            itemView.divider.visibility = View.VISIBLE
            itemView.verified_iv.visibility = if (chat.isVerified == true) {
                View.VISIBLE
            } else {
                View.GONE
            }
            itemView.bot_iv.visibility = if (chat.appId != null) {
                View.VISIBLE
            } else {
                View.GONE
            }
        } else {
            itemView.bot_iv.visibility = View.GONE
            itemView.verified_iv.visibility = View.GONE
            itemView.search_name.text = chat.groupName
            itemView.search_avatar_iv.setInfo(chat.groupName, chat.groupIconUrl, chat.conversationId)
            itemView.divider.visibility = View.VISIBLE
        }
        itemView.setOnClickListener {
            onItemClickListener?.onChatClick(chat)
        }
    }
}