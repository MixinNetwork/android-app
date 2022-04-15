package one.mixin.android.ui.conversation.holder

import one.mixin.android.R
import one.mixin.android.databinding.ItemChatStrangerBinding
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder

class StrangerHolder constructor(val binding: ItemChatStrangerBinding) : BaseViewHolder(binding.root) {

    fun bind(onItemListener: ConversationAdapter.OnItemListener, isBot: Boolean) {
        if (isBot) {
            binding.strangerInfo.setText(R.string.bot_interact_info)
            binding.strangerBlockBn.setText(R.string.bot_interact_open)
            binding.strangerAddBn.setText(R.string.bot_interact_hi)
        } else {
            binding.strangerInfo.setText(R.string.stranger_from)
            binding.strangerBlockBn.setText(R.string.Block)
            binding.strangerAddBn.setText(R.string.add_contact)
        }
        binding.strangerBlockBn.setOnClickListener {
            if (isBot) {
                onItemListener.onOpenHomePage()
            } else {
                onItemListener.onBlockClick()
            }
        }
        binding.strangerAddBn.setOnClickListener {
            if (isBot) {
                onItemListener.onSayHi()
            } else {
                onItemListener.onAddClick()
            }
        }
    }
}
