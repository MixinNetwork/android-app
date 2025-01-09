package one.mixin.android.ui.conversation.chathistory.holder

import android.util.TypedValue
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import one.mixin.android.Constants
import one.mixin.android.Constants.Colors.LINK_COLOR
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatTextQuoteBinding
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.initChatMode
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.renderMessage
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.ChatHistoryAdapter
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.widget.linktext.AutoLinkMode

class TextQuoteHolder constructor(val binding: ItemChatTextQuoteBinding) : BaseViewHolder(binding.root) {
    private val dp16 = itemView.context.dpToPx(16f)

    init {
        binding.root.context.defaultSharedPreferences.getInt(Constants.Account.PREF_TEXT_SIZE, 14).apply {
            if (this != 14) {
                val textSize = this.toFloat()
                binding.chatTime.changeSize(textSize - 4f)
                binding.chatQuote.changeSize(textSize)
                binding.chatName.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
                binding.chatTv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSize)
            }
        }
        binding.chatTv.initChatMode(LINK_COLOR)
        binding.chatTv.setSelectedStateColor(SELECT_COLOR)
        binding.chatName.setMaxWidth(itemView.context.maxItemWidth() - dp16)
        binding.chatMsgContent.setMaxWidth(itemView.context.maxItemWidth() - dp16)
    }

    override fun chatLayout(
        isMe: Boolean,
        isLast: Boolean,
        isBlink: Boolean,
    ) {
        super.chatLayout(isMe, isLast, isBlink)
        val lp = (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night,
                )
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_reply_other,
                    R.drawable.chat_bubble_reply_other_night,
                )
            }
        }
    }

    fun bind(
        messageItem: ChatHistoryMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: ChatHistoryAdapter.OnItemListener,
    ) {
        super.bind(messageItem)

        binding.chatTv.setAutoLinkOnLongClickListener { autoLinkMode, _ ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                }
                else -> {
                }
            }
        }

        binding.chatTv.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener.onUrlClick(matchedText)
                }
                AutoLinkMode.MODE_MENTION, AutoLinkMode.MODE_BOT -> {
                    onItemListener.onMentionClick(matchedText)
                }
                AutoLinkMode.MODE_PHONE -> {
                    onItemListener.onPhoneClick(matchedText)
                }
                AutoLinkMode.MODE_EMAIL -> {
                    onItemListener.onEmailClick(matchedText)
                }
                else -> {
                }
            }
        }

        if (messageItem.mentions?.isNotBlank() == true) {
            val mentionRenderContext =
                MentionRenderCache.singleton.getMentionRenderContext(
                    messageItem.mentions,
                )
            binding.chatTv.renderMessage(messageItem.content, null, mentionRenderContext)
        } else {
            binding.chatTv.renderMessage(messageItem.content, null)
        }

        val isMe = messageItem.userId == Session.getAccountId()
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.setMessageName(messageItem)
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }
        if (textQuoteGestureListener == null) {
            textQuoteGestureListener = TextQuoteGestureListener(messageItem, onItemListener)
        } else {
            textQuoteGestureListener?.apply {
                this.messageItem = messageItem
                this.onItemListener = onItemListener
            }
        }
        binding.chatContentLayout.listener = textQuoteGestureListener

        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            MessageStatus.DELIVERED.name,
            false,
            isRepresentative = false,
            isSecret = false,
        )

        binding.chatQuote.bind(fromJsonQuoteMessage(messageItem.quoteContent))

        binding.chatContentLayout.setOnClickListener {
            onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
        }

        binding.chatQuote.setOnClickListener {
            onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
        }
        chatLayout(isMe, isLast)
        binding.root.setOnLongClickListener {
            onItemListener.onMenu(binding.chatJump, messageItem)
            true
        }
        binding.chatLayout.setOnLongClickListener {
            onItemListener.onMenu(binding.chatJump, messageItem)
            true
        }
        binding.chatTv.setOnClickListener {
            onItemListener.onMenu(binding.chatJump, messageItem)
        }
        if (messageItem.transcriptId == null) {
            chatJumpLayout(binding.chatJump, isMe, messageItem.messageId, R.id.chat_layout, onItemListener)
        } else {
            binding.chatJump.isInvisible = true
            (binding.chatJump.layoutParams as ConstraintLayout.LayoutParams).apply {
                if (isMe) {
                    endToStart = R.id.chat_layout
                    startToEnd = View.NO_ID
                } else {
                    endToStart = View.NO_ID
                    startToEnd = R.id.chat_layout
                }
            }
        }
    }

    private var textQuoteGestureListener: TextQuoteGestureListener? = null

    private class TextQuoteGestureListener(
        var messageItem: ChatHistoryMessageItem,
        var onItemListener: ChatHistoryAdapter.OnItemListener,
    ) : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            onItemListener.onTextDoubleClick(messageItem)
            return true
        }
    }
}
