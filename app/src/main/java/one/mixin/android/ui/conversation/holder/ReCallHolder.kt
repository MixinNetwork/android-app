package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.View.GONE
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_recall.view.*
import one.mixin.android.R
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem

class ReCallHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    init {
        itemView.chat_flag.visibility = GONE
    }

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
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        itemView.recall_tv.text = if (isMe) {
            ctx.getString(R.string.chat_recall_me)
        } else {
            ctx.getString(R.string.chat_recall_other, messageItem.userFullName)
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
        itemView.chat_layout.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            } else {
                onItemListener.onCallClick(messageItem)
            }
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
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
        val lp = (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_me_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_me)
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            }
        }
    }
}