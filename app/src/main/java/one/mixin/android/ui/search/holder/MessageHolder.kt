package one.mixin.android.ui.search.holder

import one.mixin.android.R
import one.mixin.android.databinding.ItemSearchMessageBinding
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.ConversationCategory
import one.mixin.android.vo.SearchMessageItem

class MessageHolder constructor(val binding: ItemSearchMessageBinding) : NormalHolder(binding.root) {
    fun bind(
        message: SearchMessageItem,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        binding.searchNameTv.setName(message)
        binding.searchMsgTv.text = itemView.context.resources.getQuantityString(R.plurals.search_related_message, message.messageCount, message.messageCount)
        if (message.conversationCategory == ConversationCategory.CONTACT.name) {
            binding.searchAvatarIv.setInfo(message.userFullName, message.userAvatarUrl, message.userId)
        } else {
            binding.searchAvatarIv.setGroup(message.conversationAvatarUrl)
        }

        binding.root.setOnClickListener {
            onItemClickListener?.onMessageClick(message)
        }
    }
}
