package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.View.GONE
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_call.view.*
import one.mixin.android.R
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal

class CallHolder constructor(containerView: View) : BaseViewHolder(containerView) {

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
        itemView.chat_secret.isVisible = messageItem.isSignal()
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
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
                    ctx.getString(R.string.chat_call_declined_other)
                } else {
                    ctx.getString(R.string.chat_call_declined)
                }
            }
            MessageCategory.WEBRTC_AUDIO_END.name -> {
                val duration = try {
                    messageItem.mediaDuration?.toLong()?.formatMillis()
                } catch (e: Exception) {
                    ""
                }
                ctx.getString(R.string.chat_call_duration, duration)
            }
            MessageCategory.WEBRTC_AUDIO_BUSY.name -> {
                if (isMe) {
                    ctx.getString(R.string.chat_call_remote_busy)
                } else {
                    ctx.getString(R.string.chat_call_local_busy)
                }
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
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_me_last,
                    R.drawable.chat_bubble_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout, R.drawable.chat_bubble_me, R.drawable.chat_bubble_me_night
                )
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night
                )
            }
        }
    }
}
