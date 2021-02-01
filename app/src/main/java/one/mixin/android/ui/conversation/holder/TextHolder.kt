package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.event.MentionReadEvent
import one.mixin.android.extension.dp
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.renderMessage
import one.mixin.android.extension.tapVibrate
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal
import one.mixin.android.widget.linktext.AutoLinkMode

class TextHolder constructor(val binding: ItemChatTextBinding) : BaseMentionHolder(binding.root) {

    init {
        binding.chatTv.addAutoLinkMode(AutoLinkMode.MODE_URL, AutoLinkMode.MODE_MENTION)
        binding.chatTv.setUrlModeColor(LINK_COLOR)
        binding.chatTv.setMentionModeColor(LINK_COLOR)
        binding.chatTv.setSelectedStateColor(SELECT_COLOR)
        binding.chatLayout.setMaxWidth(itemView.context.maxItemWidth())
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        val lp = (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_me_last,
                    R.drawable.chat_bubble_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_me,
                    R.drawable.chat_bubble_me_night
                )
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night
                )
            }
        }
    }

    private var onItemListener: ConversationAdapter.OnItemListener? = null

    fun bind(
        messageItem: MessageItem,
        keyword: String?,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        this.onItemListener = onItemListener
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        binding.chatTv.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener.onUrlClick(matchedText)
                }
                AutoLinkMode.MODE_MENTION -> {
                    onItemListener.onMentionClick(matchedText)
                }
                else -> {
                }
            }
        }

        binding.chatTv.setAutoLinkOnLongClickListener { autoLinkMode, matchedText ->
            textGestureListener?.longPressed = true

            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener.onUrlLongClick(matchedText)
                }
                else -> {
                }
            }
        }

        binding.chatTv.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }

        binding.root.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }

        if (textGestureListener == null) {
            textGestureListener = TextGestureListener(messageItem, hasSelect, isSelect, onItemListener, absoluteAdapterPosition)
        } else {
            textGestureListener?.apply {
                this.messageItem = messageItem
                this.hasSelect = hasSelect
                this.isSelect = isSelect
                this.onItemListener = onItemListener
                this.absoluteAdapterPosition = this@TextHolder.absoluteAdapterPosition
            }
        }
        binding.chatLayout.listener = textGestureListener

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

        if (messageItem.mentions?.isNotBlank() == true) {
            val mentionRenderContext = MentionRenderCache.singleton.getMentionRenderContext(
                messageItem.mentions
            )
            binding.chatTv.renderMessage(messageItem.content, keyword, mentionRenderContext)
        } else {
            binding.chatTv.renderMessage(messageItem.content, keyword)
        }

        val isMe = meId == messageItem.userId
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

        if (messageItem.appId != null) {
            binding.chatName.setCompoundDrawables(null, null, botIcon, null)
            binding.chatName.compoundDrawablePadding = 3.dp
        } else {
            binding.chatName.setCompoundDrawables(null, null, null, null)
        }

        binding.dataWrapper.chatTime.timeAgoClock(messageItem.createdAt)
        setStatusIcon(
            isMe,
            messageItem.status,
            messageItem.isSignal(),
            isRepresentative
        ) { statusIcon, secretIcon, representativeIcon ->
            binding.dataWrapper.chatFlag.isVisible = statusIcon != null
            binding.dataWrapper.chatFlag.setImageDrawable(statusIcon)
            binding.dataWrapper.chatSecret.isVisible = secretIcon != null
            binding.dataWrapper.chatRepresentative.isVisible = representativeIcon != null
        }
        chatLayout(isMe, isLast)

        attachAction = if (messageItem.mentionRead == false) {
            {
                blink()
                RxBus.publish(MentionReadEvent(messageItem.conversationId, messageItem.messageId))
            }
        } else {
            null
        }
    }

    private var textGestureListener: TextGestureListener? = null

    private class TextGestureListener(
        var messageItem: MessageItem,
        var hasSelect: Boolean = false,
        var isSelect: Boolean = false,
        var onItemListener: ConversationAdapter.OnItemListener,
        var absoluteAdapterPosition: Int = 0
    ) : GestureDetector.SimpleOnGestureListener() {
        var longPressed = false

        override fun onDoubleTap(e: MotionEvent?): Boolean {
            onItemListener.onTextDoubleClick(messageItem)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
            return true
        }

        override fun onLongPress(e: MotionEvent?) {
            if (longPressed) {
                longPressed = false
                return
            }

            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                MixinApplication.appContext.tapVibrate()
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }
    }
}
