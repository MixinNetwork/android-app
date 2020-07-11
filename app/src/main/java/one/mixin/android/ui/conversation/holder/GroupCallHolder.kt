package one.mixin.android.ui.conversation.holder

import android.content.Context
import android.graphics.Color
import android.view.View
import kotlinx.android.synthetic.main.item_chat_system.view.*
import one.mixin.android.R
import one.mixin.android.extension.formatMillis
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem

class GroupCallHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    var context: Context = itemView.context

    fun bind(
        messageItem: MessageItem,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
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
        when (messageItem.type) {
            MessageCategory.KRAKEN_INVITE.name -> {
                itemView.chat_info.text = context.getString(R.string.chat_group_call_invite, messageItem.userFullName)
            }
            MessageCategory.KRAKEN_CANCEL.name -> {
                itemView.chat_info.text = context.getString(R.string.chat_group_call_cancel, messageItem.userFullName)
            }
            MessageCategory.KRAKEN_DECLINE.name -> {
                itemView.chat_info.text = context.getString(R.string.chat_group_call_decline, messageItem.userFullName)
            }
            MessageCategory.KRAKEN_END.name -> {
                val duration = try {
                    messageItem.mediaDuration?.toLong()?.formatMillis()
                } catch (e: Exception) {
                    ""
                }
                itemView.chat_info.text = context.getString(R.string.chat_group_call_end, duration)
            }
        }
    }
}
