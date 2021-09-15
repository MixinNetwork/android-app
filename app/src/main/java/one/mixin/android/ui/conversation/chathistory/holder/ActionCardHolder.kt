package one.mixin.android.ui.conversation.chathistory.holder

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatActionCardBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadRoundImage
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.TranscriptAdapter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageStatus

class ActionCardHolder constructor(val binding: ItemChatActionCardBinding) :
    BaseViewHolder(binding.root) {
    private val radius by lazy {
        4.dp
    }

    fun bind(
        messageItem: ChatHistoryMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: TranscriptAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        val isMe = messageItem.userId == Session.getAccountId()
        chatLayout(isMe, isLast)

        binding.dataWrapper.chatTime.timeAgoClock(messageItem.createdAt)
        setStatusIcon(
            isMe,
            MessageStatus.DELIVERED.name,
            isSecret = false,
            isRepresentative = false
        ) { statusIcon, secretIcon, representativeIcon ->
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
        val actionCard =
            GsonHelper.customGson.fromJson(messageItem.content, AppCardData::class.java)
        binding.chatIcon.loadRoundImage(actionCard.iconUrl, radius, R.drawable.holder_bot)
        binding.chatTitle.text = actionCard.title
        binding.chatDescription.text = actionCard.description
        itemView.setOnClickListener {
            onItemListener.onAppCardClick(actionCard, messageItem.userId)
        }
        if (messageItem.transcriptId == null) {
            chatJumpLayout(binding.chatJump, messageItem.messageId, R.id.chat_layout, onItemListener)
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
            (binding.dataWrapper.root.layoutParams as ViewGroup.MarginLayoutParams).marginEnd =
                16.dp
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
