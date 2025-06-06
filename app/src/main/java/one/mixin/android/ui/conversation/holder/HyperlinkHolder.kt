package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.Constants.Colors.LINK_COLOR
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatHyperlinkBinding
import one.mixin.android.extension.initChatMode
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.renderMessage
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.util.mention.MentionRenderCache
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret
import one.mixin.android.widget.linktext.AutoLinkMode

class HyperlinkHolder constructor(val binding: ItemChatHyperlinkBinding) : BaseViewHolder(binding.root) {
    init {
        binding.chatTv.initChatMode(LINK_COLOR)
        binding.chatTv.setSelectedStateColor(SELECT_COLOR)
        (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).matchConstraintMaxWidth = itemView.context.maxItemWidth()
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
                    binding.chatLayout,
                    R.drawable.chat_bubble_me_last,
                    R.drawable.chat_bubble_me_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_me,
                    R.drawable.chat_bubble_me_night,
                )
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night,
                )
            }
        }
    }

    private var onItemListener: MessageAdapter.OnItemListener? = null

    fun bind(
        messageItem: MessageItem,
        keyword: String?,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: MessageAdapter.OnItemListener,
    ) {
        super.bind(messageItem)
        this.onItemListener = onItemListener
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        binding.chatTv.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }

        binding.chatTv.setAutoLinkOnLongClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener.onUrlLongClick(matchedText)
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

        binding.chatTv.setOnClickListener {
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

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }

        if (messageItem.mentions?.isNotBlank() == true) {
            val mentionRenderContext =
                MentionRenderCache.singleton.getMentionRenderContext(
                    messageItem.mentions,
                )
            binding.chatTv.renderMessage(messageItem.content, keyword, mentionRenderContext)
        } else {
            binding.chatTv.renderMessage(messageItem.content, keyword)
        }

        val isMe = meId == messageItem.userId
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.setMessageName(messageItem)
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }

        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            messageItem.status,
            messageItem.isPin ?: false,
            isRepresentative = isRepresentative,
            isSecret = messageItem.isSecret(),
        )

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }

        binding.chatNameTv.visibility =
            if (messageItem.siteName.isNullOrBlank()) {
                View.GONE
            } else {
                binding.chatNameTv.text = messageItem.siteName
                View.VISIBLE
            }

        binding.chatDescriptionTv.visibility =
            if (messageItem.siteDescription.isNullOrBlank()) {
                View.GONE
            } else {
                binding.chatDescriptionTv.text = messageItem.siteDescription
                View.VISIBLE
            }
        chatLayout(isMe, isLast)
    }
}
