package one.mixin.android.ui.conversation.transcript.holder

import android.view.Gravity
import android.view.View
import androidx.core.widget.TextViewCompat
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatImageQuoteBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadLongImageMark
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.holder.MediaHolder
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip
import kotlin.math.min

class ImageQuoteHolder constructor(val binding: ItemChatImageQuoteBinding) : MediaHolder(binding.root) {
    private val dp16 = itemView.context.dpToPx(16f)

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        binding.chatImageLayout.round(radius)
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
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onClickListener: View.OnClickListener
    ) {
        super.bind(messageItem)
        binding.chatQuoteLayout.setRatio(messageItem.mediaWidth!!.toFloat() / messageItem.mediaHeight!!.toFloat())

        binding.chatTime.timeAgoClock(messageItem.createdAt)

        binding.chatWarning.visibility = View.GONE
        binding.progress.visibility = View.GONE
        binding.progress.setBindId(messageItem.messageId)
        binding.progress.setOnClickListener {}
        binding.progress.setOnLongClickListener { false }
        binding.chatImage.setOnClickListener(onClickListener)

        val dataWidth = messageItem.mediaWidth
        val dataHeight = messageItem.mediaHeight
        val width = mediaWidth - dp6
        if (dataWidth <= 0 || dataHeight <= 0) {
            binding.chatImage.layoutParams.width = width
            binding.chatImage.layoutParams.height = width
        } else {
            binding.chatImage.layoutParams.width = width
            binding.chatImage.layoutParams.height =
                min(width * dataHeight / dataWidth, mediaHeight)
        }
        binding.chatImage.loadLongImageMark(messageItem.mediaUrl, null)

        val isMe = false
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
        } else {
            binding.chatName.visibility = View.GONE
        }

        if (messageItem.appId != null) {
            binding.chatName.setCompoundDrawables(null, null, botIcon, null)
            binding.chatName.compoundDrawablePadding = itemView.dip(3)
        } else {
            binding.chatName.setCompoundDrawables(null, null, null, null)
        }
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), isRepresentative = false, isWhite = true) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, secretIcon ?: representativeIcon, null, statusIcon, null)
        }
        val quoteMessage = GsonHelper.customGson.fromJson(messageItem.quoteContent, QuoteMessageItem::class.java)
        binding.chatQuote.bind(quoteMessage)

        chatLayout(isMe, isLast)
    }
}
