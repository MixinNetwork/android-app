package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatCallBinding
import one.mixin.android.extension.formatMillis
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem

class CallHolder constructor(val binding: ItemChatCallBinding) : BaseViewHolder(binding.root) {

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        val ctx = itemView.context
        val isMe = meId == messageItem.userId
        chatLayout(isMe, isLast)
        binding.chatTime.load(
            isMe, messageItem.createdAt,
            null,
            isPin = false,
            isRepresentative = false,
            isSecret = false
        )
        binding.callTv.text = when (messageItem.type) {
            MessageCategory.WEBRTC_AUDIO_CANCEL.name -> {
                if (isMe) {
                    ctx.getString(R.string.Canceled)
                } else {
                    ctx.getString(R.string.Canceled_by_caller)
                }
            }
            MessageCategory.WEBRTC_AUDIO_DECLINE.name -> {
                if (isMe) {
                    ctx.getString(R.string.Call_declined)
                } else {
                    ctx.getString(R.string.Declined)
                }
            }
            MessageCategory.WEBRTC_AUDIO_END.name -> {
                val duration = messageItem.mediaDuration?.toLongOrNull()?.formatMillis() ?: ""
                ctx.getString(R.string.chat_call_duration, duration)
            }
            MessageCategory.WEBRTC_AUDIO_BUSY.name -> {
                if (isMe) {
                    ctx.getString(R.string.Line_busy_remote)
                } else {
                    ctx.getString(R.string.Line_busy)
                }
            }
            else -> {
                ctx.getString(R.string.Call_failed)
            }
        }

        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        binding.chatLayout.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onCallClick(messageItem)
            }
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }
        binding.chatLayout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        val lp = (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_me_last,
                    R.drawable.chat_bubble_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_me,
                    R.drawable.chat_bubble_me_night
                )
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night
                )
            }
        }
    }
}
