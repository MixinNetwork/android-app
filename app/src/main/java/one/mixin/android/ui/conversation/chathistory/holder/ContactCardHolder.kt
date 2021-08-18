package one.mixin.android.ui.conversation.chathistory.holder

import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatContactCardBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.TranscriptAdapter
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.TranscriptMessageItem
import one.mixin.android.vo.showVerifiedOrBot

class ContactCardHolder(val binding: ItemChatContactCardBinding) : BaseViewHolder(binding.root) {

    fun bind(
        messageItem: TranscriptMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: TranscriptAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        binding.avatarIv.setInfo(
            messageItem.sharedUserFullName,
            messageItem.sharedUserAvatarUrl,
            messageItem.sharedUserId ?: "0"
        )
        val isMe = messageItem.userId == Session.getAccountId()
        binding.nameTv.text = messageItem.sharedUserFullName
        binding.idTv.text = messageItem.sharedUserIdentityNumber
        messageItem.showVerifiedOrBot(binding.verifiedIv, binding.botIv)

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
        setStatusIcon(isMe, MessageStatus.DELIVERED.name, false, false) { statusIcon, secretIcon, representativeIcon ->
            binding.dataWrapper.chatFlag.isVisible = statusIcon != null
            binding.dataWrapper.chatFlag.setImageDrawable(statusIcon)
            binding.dataWrapper.chatSecret.isVisible = secretIcon != null
            binding.dataWrapper.chatRepresentative.isVisible = representativeIcon != null
        }
        chatLayout(isMe, isLast)
        binding.chatContentLayout.setOnClickListener {
            onItemListener.onContactCardClick(messageItem.sharedUserId!!)
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
            (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
            (binding.dataWrapper.root.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 16.dp
        } else {
            (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
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
