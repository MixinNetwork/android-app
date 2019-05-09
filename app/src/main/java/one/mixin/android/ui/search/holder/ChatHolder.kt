package one.mixin.android.ui.search.holder

import android.view.View
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_search_contact.view.*
import one.mixin.android.R
import one.mixin.android.extension.highLight
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationCategory

class ChatHolder constructor(containerView: View) : NormalHolder(containerView) {
    init {
        itemView.bot_iv.visibility = View.GONE
        itemView.verified_iv.visibility = View.GONE
    }

    fun bind(chat: ChatMinimal, target: String?, onItemClickListener: SearchFragment.OnSearchClickListener?,
        isEnd: Boolean = false, isLast: Boolean = false) {
        itemView.ph1.isVisible = isEnd
        if (isLast) {
            itemView.ph2.setBackgroundResource(R.drawable.ic_shadow_bottom)
        } else {
            itemView.ph2.setBackgroundResource(R.drawable.ic_shadow_divider)
        }
        itemView.ph2.isVisible = isEnd
        if (chat.category == ConversationCategory.CONTACT.name) {
            itemView.search_name.text = chat.fullName
            itemView.search_name.highLight(target)
            itemView.search_avatar_iv.setInfo(chat.fullName, chat.avatarUrl, chat.userId)
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
            itemView.search_name.highLight(target)
            itemView.search_avatar_iv.setInfo(chat.groupName, chat.groupIconUrl, chat.conversationId)
        }
        itemView.setOnClickListener {
            onItemClickListener?.onChatClick(chat)
        }
    }
}