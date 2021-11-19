package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatActionCardBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadRoundImage
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret

class ActionCardHolder constructor(val binding: ItemChatActionCardBinding) :
    BaseViewHolder(binding.root) {
    private val radius by lazy {
        4.dp
    }

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
        val isMe = meId == messageItem.userId
        chatLayout(isMe, isLast)
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
        binding.dataWrapper.chatTime.timeAgoClock(messageItem.createdAt)
        setStatusIcon(isMe, messageItem.status, messageItem.isSecret(), isRepresentative) { statusIcon, secretIcon, representativeIcon ->
            binding.dataWrapper.chatFlag.isVisible = statusIcon != null
            binding.dataWrapper.chatFlag.setImageDrawable(statusIcon)
            binding.dataWrapper.chatSecret.isVisible = secretIcon != null
            binding.dataWrapper.chatRepresentative.isVisible = representativeIcon != null
        }
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
        val actionCard = requireNotNull(getTypeAdapter<AppCardData>(AppCardData::class.java).fromJson(messageItem.content!!))
        binding.chatIcon.loadRoundImage(actionCard.iconUrl, radius, R.drawable.holder_bot)
        binding.chatTitle.text = actionCard.title
        binding.chatDescription.text = actionCard.description
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onAppCardClick(actionCard, messageItem.userId)
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
