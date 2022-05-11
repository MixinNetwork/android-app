package one.mixin.android.ui.conversation.holder

import android.content.Context
import android.graphics.Color
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatSystemBinding
import one.mixin.android.extension.renderMessage
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.PinMessageMinimal
import one.mixin.android.vo.explain

class PinMessageHolder constructor(val binding: ItemChatSystemBinding) :
    BaseViewHolder(binding.root) {

    var context: Context = itemView.context
    private fun getText(id: Int) = context.getText(id).toString()

    fun bind(
        messageItem: MessageItem,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        val id = meId
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        val pinMessage = try {
            GsonHelper.customGson.fromJson(messageItem.content, PinMessageMinimal::class.java)
        } catch (e: Exception) {
            null
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
            } else {
                onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
            }
        }

        if (messageItem.mentions != null) {
            binding.chatInfo.renderMessage(
                String.format(
                    getText(R.string.chat_pin_message),
                    if (id == messageItem.userId) {
                        getText(R.string.You)
                    } else {
                        messageItem.userFullName
                    },
                    pinMessage?.let { msg ->
                        " \"${msg.content}\""
                    } ?: getText(R.string.a_message)
                ),
                MentionRenderCache.singleton.getMentionRenderContext(
                    messageItem.mentions
                )
            )
        } else {
            binding.chatInfo.text =
                String.format(
                    getText(R.string.chat_pin_message),
                    if (id == messageItem.userId) {
                        getText(R.string.You)
                    } else {
                        messageItem.userFullName
                    },
                    pinMessage.explain(binding.root.context)
                )
        }
    }
}
