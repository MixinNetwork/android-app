package one.mixin.android.ui.search.holder

import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchMessageBinding
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.SearchMessageItem
import one.mixin.android.vo.membershipIcon

class MessageHolder constructor(val binding: ItemSearchMessageBinding) : NormalHolder(binding.root) {
    fun bind(
        message: SearchMessageItem,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        binding.searchNameTv.text =
            if (message.conversationName.isNullOrEmpty()) {
                message.userFullName
            } else {
                message.conversationName
            }
        binding.searchMsgTv.text = itemView.context.resources.getQuantityString(R.plurals.search_related_message, message.messageCount, message.messageCount)
        if (message.conversationCategory == ConversationCategory.CONTACT.name) {
            binding.searchAvatarIv.setInfo(message.userFullName, message.userAvatarUrl, message.userId)
        } else {
            binding.searchAvatarIv.setGroup(message.conversationAvatarUrl)
        }
        if (message.isMembership()) {
            binding.badge.isVisible = true
            binding.badge.setImageResource(message.membership.membershipIcon())
        } else {
            binding.badge.isVisible = false
        }

        binding.root.setOnClickListener {
            onItemClickListener?.onMessageClick(message)
        }
    }
}
