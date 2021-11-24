package one.mixin.android.ui.conversation.chathistory.holder

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatTextQuoteBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.renderMessage
import one.mixin.android.moshi.MoshiHelper.getTypeAdapter
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.TranscriptAdapter
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.widget.linktext.AutoLinkMode
import org.jetbrains.anko.dip

class TextQuoteHolder constructor(val binding: ItemChatTextQuoteBinding) : BaseViewHolder(binding.root) {
    private val dp16 = itemView.context.dpToPx(16f)

    init {
        binding.chatTv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        binding.chatTv.setUrlModeColor(LINK_COLOR)
        binding.chatTv.setMentionModeColor(LINK_COLOR)
        binding.chatTv.setSelectedStateColor(SELECT_COLOR)
        binding.chatName.maxWidth = itemView.context.maxItemWidth() - dp16
        binding.chatMsgContent.setMaxWidth(itemView.context.maxItemWidth() - dp16)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        val lp = (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night
                )
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatContentLayout,
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

        binding.chatTv.setAutoLinkOnLongClickListener { autoLinkMode, matchedText ->
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
                else -> {
                }
            }
        }

        if (messageItem.mentions?.isNotBlank() == true) {
            val mentionRenderContext = MentionRenderCache.singleton.getMentionRenderContext(
                messageItem.mentions
            )
            binding.chatTv.renderMessage(messageItem.content, null, mentionRenderContext)
        } else {
            binding.chatTv.renderMessage(messageItem.content, null)
        }

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
        if (textQuoteGestureListener == null) {
            textQuoteGestureListener = TextQuoteGestureListener(messageItem, onItemListener)
        } else {
            textQuoteGestureListener?.apply {
                this.messageItem = messageItem
                this.onItemListener = onItemListener
            }
        }
        binding.chatContentLayout.listener = textQuoteGestureListener

        if (messageItem.appId != null) {
            binding.chatName.setCompoundDrawables(null, null, botIcon, null)
            binding.chatName.compoundDrawablePadding = itemView.dip(3)
        } else {
            binding.chatName.setCompoundDrawables(null, null, null, null)
        }
        messageItem.quoteContent?.let { quoteContent ->
            binding.chatQuote.bind(
                getTypeAdapter<QuoteMessageItem>(QuoteMessageItem::class.java).fromJson(quoteContent)
            )
        }
        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            MessageStatus.DELIVERED.name,
            false,
            isRepresentative = false,
            isSecret = false
        )

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
        var onItemListener: TranscriptAdapter.OnItemListener,
    ) : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            onItemListener.onTextDoubleClick(messageItem)
            return true
        }
    }
}
