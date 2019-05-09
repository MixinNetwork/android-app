package one.mixin.android.ui.search.holder

import android.view.View
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.item_search_message.view.*
import one.mixin.android.R
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.SearchMessageItem

class MessageHolder constructor(containerView: View) : NormalHolder(containerView) {

    fun bind(message: SearchMessageItem, onItemClickListener: SearchFragment.OnSearchClickListener?,
        isEnd: Boolean = false, isLast: Boolean = false) {
        itemView.ph1.isVisible = isEnd
        if (isLast) {
            itemView.ph2.setBackgroundResource(R.drawable.ic_shadow_bottom)
        } else {
            itemView.ph2.setBackgroundResource(R.drawable.ic_shadow_divider)
        }
        itemView.ph2.isVisible = isEnd
        itemView.search_name_tv.text = if (message.conversationName.isNullOrEmpty()) {
            message.userFullName
        } else {
            message.conversationName
        }
        itemView.search_msg_tv.text = itemView.context.getString(R.string.search_related_message, message.messageCount)
        if (message.conversationCategory == ConversationCategory.CONTACT.name) {
            itemView.search_avatar_iv.setInfo(message.userFullName, message.userAvatarUrl, message.userId)
        } else {
            itemView.search_avatar_iv.setGroup(message.conversationAvatarUrl)
        }
        itemView.setOnClickListener {
            onItemClickListener?.onMessageClick(message)
        }
    }
}