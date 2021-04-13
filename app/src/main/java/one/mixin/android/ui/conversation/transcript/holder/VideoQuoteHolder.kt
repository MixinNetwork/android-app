package one.mixin.android.ui.conversation.transcript.holder

import android.view.Gravity
import android.view.View
import androidx.core.widget.TextViewCompat
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatVideoQuoteBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadVideo
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.realSize
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip

class VideoQuoteHolder constructor(val binding: ItemChatVideoQuoteBinding) : BaseViewHolder(binding.root) {
    private val dp16 = itemView.context.dpToPx(16f)
    private val minWidth by lazy {
        (itemView.context.realSize().x * 0.5).toInt()
    }

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
        when (messageItem.mediaStatus) {
            MediaStatus.DONE.name -> {

                binding.durationTv.bindId(null)
                messageItem.mediaDuration.notNullWithElse(
                    {
                        binding.durationTv.visibility = View.VISIBLE
                        binding.durationTv.text = it.toLongOrNull()?.formatMillis() ?: ""
                    },
                    {
                        binding.durationTv.visibility = View.GONE
                    }
                )
            }
            MediaStatus.PENDING.name -> {
                messageItem.mediaSize.notNullWithElse(
                    {
                        binding.durationTv.visibility = View.VISIBLE
                        if (it == 0L) {
                            binding.durationTv.bindId(messageItem.messageId)
                        } else {
                            binding.durationTv.text = it.fileSize()
                            binding.durationTv.bindId(null)
                        }
                    },
                    {
                        binding.durationTv.bindId(null)
                        binding.durationTv.visibility = View.GONE
                    }
                )
            }
            else -> {
                messageItem.mediaSize.notNullWithElse(
                    {
                        if (it == 0L) {
                            binding.durationTv.visibility = View.GONE
                        } else {
                            binding.durationTv.visibility = View.VISIBLE
                            binding.durationTv.text = it.fileSize()
                        }
                    },
                    {
                        binding.durationTv.visibility = View.GONE
                    }
                )
                binding.durationTv.bindId(null)
            }
        }

        binding.chatWarning.visibility = View.GONE
        binding.progress.visibility = View.GONE
        binding.play.visibility = View.VISIBLE
        binding.progress.setBindId(messageItem.messageId)
        binding.progress.setOnClickListener {}
        binding.progress.setOnLongClickListener { false }

        binding.chatImage.setOnClickListener(onClickListener)

        binding.chatImage.loadVideo(
            messageItem.mediaUrl,
            messageItem.thumbImage,
            minWidth,
            minWidth * messageItem.mediaHeight / messageItem.mediaWidth
        )

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
