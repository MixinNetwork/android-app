package one.mixin.android.ui.conversation.holder

import one.mixin.android.databinding.ItemChatCardBinding
import one.mixin.android.vo.MessageItem

class CardHolder constructor(val binding: ItemChatCardBinding) : BaseViewHolder(binding.root) {

    public override fun bind(messageItem: MessageItem) {
        super.bind(messageItem)
        binding.nameTv.text = messageItem.content
    }
}
