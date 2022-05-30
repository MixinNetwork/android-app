package one.mixin.android.ui.conversation.holder

import android.view.ViewGroup
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatStrangerBinding
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder

class StrangerHolder constructor(val binding: ItemChatStrangerBinding) : BaseViewHolder(binding.root) {

    fun bind(onItemListener: ConversationAdapter.OnItemListener, inviterId: String?, isBot: Boolean) {
        if (inviterId != null) {
            binding.strangerInfo.setText(R.string.Invited_by_Stranger)
            binding.strangerBlockBn.setText(R.string.Exit_group_and_report_inviter)
            binding.strangerAddBn.isVisible = false
            (binding.strangerBlockBn.layoutParams as ViewGroup.MarginLayoutParams).apply {
                width = ViewGroup.LayoutParams.WRAP_CONTENT
                marginStart = 0
                marginEnd = 0
            }
        } else if (isBot) {
            binding.strangerInfo.setText(R.string.chat_bot_reception_title)
            binding.strangerBlockBn.setText(R.string.Open_Home_page)
            binding.strangerAddBn.setText(R.string.Say_Hi)
            binding.strangerAddBn.isVisible = true
        } else {
            binding.strangerInfo.setText(R.string.stranger_hint)
            binding.strangerBlockBn.setText(R.string.Block)
            binding.strangerAddBn.setText(R.string.Add_Contact)
            binding.strangerAddBn.isVisible = true
        }
        binding.strangerBlockBn.setOnClickListener {
            if (inviterId != null) {
                onItemListener.onExitAndReport(inviterId)
            } else if (isBot) {
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
