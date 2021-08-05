package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatContactCardBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret
import one.mixin.android.vo.showVerifiedOrBot

class ContactCardHolder(val binding: ItemChatContactCardBinding) : BaseViewHolder(binding.root) {

    fun bind(
        messageItem: MessageItem,
        isFirst: Boolean,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        binding.avatarIv.setInfo(
            messageItem.sharedUserFullName,
            messageItem.sharedUserAvatarUrl,
            messageItem.sharedUserId
                ?: "0"
        )
        binding.nameTv.text = messageItem.sharedUserFullName
        binding.idTv.text = messageItem.sharedUserIdentityNumber
        messageItem.showVerifiedOrBot(binding.verifiedIv, binding.botIv)

        val isMe = Session.getAccountId() == messageItem.userId
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = 3.dp
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }

        binding.dataWrapper.chatTime.timeAgoClock(messageItem.createdAt)
        setStatusIcon(isMe, messageItem.status, messageItem.isSecret(), isRepresentative) { statusIcon, secretIcon, representativeIcon ->
            binding.dataWrapper.chatFlag.isVisible = statusIcon != null
            binding.dataWrapper.chatFlag.setImageDrawable(statusIcon)
            binding.dataWrapper.chatSecret.isVisible = secretIcon != null
            binding.dataWrapper.chatRepresentative.isVisible = representativeIcon != null
        }
        chatLayout(isMe, isLast)

        binding.chatContentLayout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onContactCardClick(messageItem.sharedUserId!!)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
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
        binding.chatContentLayout.setOnLongClickListener {
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
        if (isMe) {
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.bill_bubble_me_last,
                    R.drawable.bill_bubble_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.bill_bubble_me,
                    R.drawable.bill_bubble_me_night
                )
            }
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.dataWrapper.root.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 16.dp
        } else {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (binding.dataWrapper.root.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 8.dp
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night
                )
            }
        }
    }
}
