package one.mixin.android.ui.search.holder

import one.mixin.android.databinding.ItemSearchChatBinding
import one.mixin.android.extension.highLight
import one.mixin.android.ui.common.recyclerview.NormalHolder
import one.mixin.android.ui.search.SearchFragment
import one.mixin.android.vo.ChatMinimal
import one.mixin.android.vo.ConversationCategory

class ChatHolder(val binding: ItemSearchChatBinding) : NormalHolder(binding.root) {

    fun bind(
        chat: ChatMinimal,
        target: String?,
        onItemClickListener: SearchFragment.OnSearchClickListener?,
    ) {
        if (chat.category == ConversationCategory.CONTACT.name) {
            binding.searchName.highLight(target)
            binding.searchAvatarIv.setInfo(chat.fullName, chat.avatarUrl, chat.userId)
            binding.searchName.setName(chat)
        } else {
            binding.searchName.setTextOnly(chat.groupName)
            binding.searchName.highLight(target)
            binding.searchAvatarIv.setInfo(chat.groupName, chat.groupIconUrl, chat.conversationId)
        }
        binding.root.setOnClickListener {
            onItemClickListener?.onChatClick(chat)
        }
        binding.root.setOnLongClickListener {
            return@setOnLongClickListener onItemClickListener?.onChatLongClick(chat, binding.searchName) ?: false
        }
    }
}
