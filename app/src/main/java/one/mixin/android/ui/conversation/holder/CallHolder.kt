package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.item_chat_call.view.*
import one.mixin.android.R
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.timeAgo
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem

class CallHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    private var onItemListener: ConversationAdapter.OnItemListener? = null

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
        itemView.call_tv.text = when (messageItem.type) {
            MessageCategory.WEBRTC_AUDIO_CANCEL.name -> {
                if (isMe) {
                    ctx.getString(R.string.chat_call_canceled)
                } else {
                    ctx.getString(R.string.chat_call_canceled_by_caller)
                }
            }
            MessageCategory.WEBRTC_AUDIO_DECLINE.name -> {
                if (isMe) {
                    ctx.getString(R.string.chat_call_declined)
                } else {
                    ctx.getString(R.string.chat_call_declined_other)
                }
            }
            MessageCategory.WEBRTC_AUDIO_END.name -> {
                val duration = messageItem.mediaDuration!!.toLong().formatMillis()
                ctx.getString(R.string.chat_call_duration, duration)
            }
            MessageCategory.WEBRTC_AUDIO_BUSY.name -> {
                ctx.getString(R.string.chat_call_busy)
            }
            else -> {
                ctx.getString(R.string.chat_call_failed)
            }
        }

        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }
        itemView.chat_layout.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            } else {
                onItemListener.onBillClick(messageItem)
            }
        }
        itemView.chat_layout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.bill_bubble_me_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.bill_bubble_me)
            }
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
        } else {
            (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            }
        }
    }
}