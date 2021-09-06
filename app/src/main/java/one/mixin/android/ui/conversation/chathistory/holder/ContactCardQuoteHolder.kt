package one.mixin.android.ui.conversation.chathistory.holder

import android.view.Gravity
import android.view.View
import androidx.core.widget.TextViewCompat
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatContactCardQuoteBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.TranscriptAdapter
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.SnakeQuoteMessageItem
import one.mixin.android.vo.showVerifiedOrBot
import org.jetbrains.anko.dip

class ContactCardQuoteHolder constructor(val binding: ItemChatContactCardQuoteBinding) : MediaHolder(binding.root) {

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        binding.chatTime.round(radius)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            binding.chatMsgLayout.gravity = Gravity.END
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night
                )
            }
        } else {
            binding.chatMsgLayout.gravity = Gravity.START
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_other,
                    R.drawable.chat_bubble_reply_other_night
                )
            }
        }
    }

    fun bind(
        messageItem: ChatHistoryMessageItem,
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
        binding.nameTv.text = messageItem.sharedUserFullName
        binding.idTv.text = messageItem.sharedUserIdentityNumber
        binding.chatTime.timeAgoClock(messageItem.createdAt)
        messageItem.showVerifiedOrBot(binding.verifiedIv, binding.botIv)

        val isMe = messageItem.userId == Session.getAccountId()
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

        chatLayout(isMe, isLast)

        binding.chatLayout.setOnClickListener {
            onItemListener.onContactCardClick(messageItem.sharedUserId!!)
        }

        try {
            binding.chatQuote.bind(GsonHelper.customGson.fromJson(messageItem.quoteContent, SnakeQuoteMessageItem::class.java))
        } catch (e: Exception) {
            binding.chatQuote.bind(GsonHelper.customGson.fromJson(messageItem.quoteContent, QuoteMessageItem::class.java))
        }

        binding.chatQuote.setOnClickListener {
            onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
        }

        setStatusIcon(isMe, MessageStatus.DELIVERED.name, isSecret = false, isRepresentative = false) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, secretIcon ?: representativeIcon, null, statusIcon, null)
        }
    }
}
