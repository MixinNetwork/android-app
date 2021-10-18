package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import io.noties.markwon.Markwon
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatPostBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.postLengthOptimize
import one.mixin.android.extension.postOptimize
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret
import org.jetbrains.anko.dip

class PostHolder constructor(val binding: ItemChatPostBinding) : BaseViewHolder(binding.root) {
    init {
        binding.chatTv.layoutParams.width = itemView.context.maxItemWidth()
        binding.chatTv.maxHeight = itemView.context.maxItemWidth() * 10 / 16
        binding.chatTv.round(3.dp)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 12.dp
            (binding.chatPost.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 12.dp
            (binding.chatTv.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 8.dp
                marginEnd = 14.dp
            }
        } else {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 6.dp
            (binding.chatPost.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 6.dp
            (binding.chatTv.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 14.dp
                marginEnd = 8.dp
            }
        }
        val lp = (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_post_me_last,
                    R.drawable.chat_bubble_post_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_post_me,
                    R.drawable.chat_bubble_post_me_night
                )
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_post_other_last,
                    R.drawable.chat_bubble_post_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
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
        isRepresentative: Boolean,
        onItemListener: ConversationAdapter.OnItemListener,
        miniMarkwon: Markwon
    ) {
        super.bind(messageItem)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        binding.chatTv.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }

        binding.chatTv.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onPostClick(itemView, messageItem)
            }
        }
        binding.chatLayout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onPostClick(itemView, messageItem)
            }
        }

        if (binding.chatTv.tag != messageItem.content.hashCode()) {
            if (!messageItem.thumbImage.isNullOrEmpty()) {
                miniMarkwon.setMarkdown(binding.chatTv, messageItem.thumbImage.postLengthOptimize())
                binding.chatTv.tag = messageItem.content.hashCode()
            } else if (!messageItem.content.isNullOrEmpty()) {
                miniMarkwon.setMarkdown(binding.chatTv, messageItem.content.postOptimize())
                binding.chatTv.tag = messageItem.content.hashCode()
            } else {
                binding.chatTv.text = null
                binding.chatTv.tag = null
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

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }

        val isMe = meId == messageItem.userId
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = itemView.dip(3)
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }

        if (messageItem.appId != null) {
            binding.chatName.setCompoundDrawables(null, null, botIcon, null)
            binding.chatName.compoundDrawablePadding = itemView.dip(3)
        } else {
            binding.chatName.setCompoundDrawables(null, null, null, null)
        }
        binding.chatTime.timeAgoClock(messageItem.createdAt)
        setStatusIcon(isMe, messageItem.status, messageItem.isSecret(), isRepresentative, true) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, 12.dp, 12.dp)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, secretIcon ?: representativeIcon, null, statusIcon, null)
        }
        chatLayout(isMe, isLast)
    }
}
