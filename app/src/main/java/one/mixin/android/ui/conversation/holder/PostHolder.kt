package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import io.noties.markwon.Markwon
import kotlinx.android.synthetic.main.item_chat_action.view.chat_name
import kotlinx.android.synthetic.main.item_chat_post.view.*
import one.mixin.android.R
import one.mixin.android.extension.dp
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.postLengthOptimize
import one.mixin.android.extension.postOptimize
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip

class PostHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    init {
        itemView.chat_tv.layoutParams.width = itemView.context.maxItemWidth()
        itemView.chat_tv.maxHeight = itemView.context.maxItemWidth() * 10 / 16
        itemView.chat_tv.round(3.dp)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (itemView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 12.dp
            (itemView.chat_post.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 12.dp
            (itemView.chat_tv.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 8.dp
                marginEnd = 14.dp
            }
        } else {
            (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (itemView.chat_time.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 6.dp
            (itemView.chat_post.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 6.dp
            (itemView.chat_tv.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 14.dp
                marginEnd = 8.dp
            }
        }
        val lp = (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_post_me_last,
                    R.drawable.chat_bubble_post_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_post_me,
                    R.drawable.chat_bubble_post_me_night
                )
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_post_other_last,
                    R.drawable.chat_bubble_post_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_post_other,
                    R.drawable.chat_bubble_post_other_night
                )
            }
        }
    }

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener,
        miniMarkwon: Markwon
    ) {
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        itemView.chat_tv.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }

        itemView.chat_tv.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            } else {
                onItemListener.onPostClick(itemView, messageItem)
            }
        }
        itemView.chat_layout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onPostClick(itemView, messageItem)
            }
        }

        if (itemView.chat_tv.tag != messageItem.content.hashCode()) {
            if (!messageItem.thumbImage.isNullOrEmpty()) {
                miniMarkwon.setMarkdown(itemView.chat_tv, messageItem.thumbImage.postLengthOptimize())
                itemView.chat_tv.tag = messageItem.content.hashCode()
            } else if (!messageItem.content.isNullOrEmpty()) {
                miniMarkwon.setMarkdown(itemView.chat_tv, messageItem.content.postOptimize())
                itemView.chat_tv.tag = messageItem.content.hashCode()
            } else {
                itemView.chat_tv.text = null
                itemView.chat_tv.tag = null
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

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        val isMe = meId == messageItem.userId
        if (isFirst && !isMe) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = messageItem.userFullName
            if (messageItem.appId != null) {
                itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
                itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
            } else {
                itemView.chat_name.setCompoundDrawables(null, null, null, null)
            }
            itemView.chat_name.setTextColor(getColorById(messageItem.userId))
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }

        if (messageItem.appId != null) {
            itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
            itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
        } else {
            itemView.chat_name.setCompoundDrawables(null, null, null, null)
        }
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, 12.dp, 12.dp)
            secretIcon?.setBounds(0, 0, 8.dp, 8.dp)
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, secretIcon, null, statusIcon, null)
        }
        chatLayout(isMe, isLast)
    }
}
