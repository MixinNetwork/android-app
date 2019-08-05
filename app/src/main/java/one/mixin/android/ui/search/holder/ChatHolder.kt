package one.mixin.android.ui.search.holder

import android.view.View
import kotlinx.android.synthetic.main.item_search_contact.view.*
import one.mixin.android.extension.highLight
import one.mixin.android.extension.nonBlankFullName
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.showVerifiedOrBot

class ChatHolder constructor(containerView: View) : NormalHolder(containerView) {
    init {
        itemView.bot_iv.visibility = View.GONE
        itemView.verified_iv.visibility = View.GONE
    }

    fun bind(chat: ChatMinimal, target: String?, onItemClickListener: SearchFragment.OnSearchClickListener?) {
        if (chat.category == ConversationCategory.CONTACT.name) {
            itemView.search_name.text = chat.fullName.nonBlankFullName(chat.ownerIdentityNumber)
            itemView.search_name.highLight(target)
            itemView.search_avatar_iv.setInfo(chat.fullName, chat.avatarUrl, chat.userId)
            chat.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)
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
