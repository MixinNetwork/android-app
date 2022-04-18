package one.mixin.android.ui.conversation.holder

import android.content.Context
import android.graphics.Color
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatSystemBinding
import one.mixin.android.extension.formatMillis
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem

class GroupCallHolder constructor(val binding: ItemChatSystemBinding) : BaseViewHolder(binding.root) {

    var context: Context = itemView.context

    fun bind(
        messageItem: MessageItem,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }
        val isMe = meId == messageItem.userId
        val name = if (isMe) {
            context.getString(R.string.You)
        } else {
            messageItem.userFullName
        }
        when (messageItem.type) {
            MessageCategory.KRAKEN_INVITE.name -> {
                binding.chatInfo.text = context.getString(R.string.chat_group_call_invite, messageItem.userFullName)
            }
            MessageCategory.KRAKEN_CANCEL.name -> {
                binding.chatInfo.text = context.getString(R.string.chat_group_call_cancel, name)
            }
            MessageCategory.KRAKEN_DECLINE.name -> {
                binding.chatInfo.text = context.getString(R.string.chat_group_call_decline, name)
            }
            MessageCategory.KRAKEN_END.name -> {
                val duration = try {
                    messageItem.mediaDuration?.toLong()?.formatMillis()
                } catch (e: Exception) {
                    ""
                }
                binding.chatInfo.text = context.getString(R.string.group_call_end_with_duration, duration)
            }
        }
    }
}
