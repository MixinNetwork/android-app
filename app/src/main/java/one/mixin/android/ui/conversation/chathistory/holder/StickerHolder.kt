package one.mixin.android.ui.conversation.chathistory.holder

import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatStickerBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadSticker
import one.mixin.android.extension.round
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.ChatHistoryAdapter
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MessageStatus

class StickerHolder constructor(val binding: ItemChatStickerBinding) : BaseViewHolder(binding.root) {
    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        binding.chatSticker.round(radius)
    }

    private val dp120 by lazy {
        itemView.context.dpToPx(120f)
    }

    private val dp64 by lazy {
        itemView.context.dpToPx(64f)
    }

    fun bind(
        messageItem: ChatHistoryMessageItem,
        isFirst: Boolean,
        onItemListener: ChatHistoryAdapter.OnItemListener,
    ) {
        super.bind(messageItem)
        val isMe = messageItem.userId == Session.getAccountId()
        if (messageItem.assetWidth == null || messageItem.assetHeight == null) {
            binding.chatSticker.layoutParams.width = dp120
            binding.chatSticker.layoutParams.height = dp120
            binding.chatTime.visibility = INVISIBLE
        } else if (messageItem.assetWidth * 2 < dp64 || messageItem.assetHeight * 2 < dp64) {
            if (messageItem.assetWidth < messageItem.assetHeight) {
                if (dp64 * messageItem.assetHeight / messageItem.assetWidth > dp120) {
                    binding.chatSticker.layoutParams.width = dp120 * messageItem.assetWidth / messageItem.assetHeight
                    binding.chatSticker.layoutParams.height = dp120
                } else {
                    binding.chatSticker.layoutParams.width = dp64
                    binding.chatSticker.layoutParams.height = dp64 * messageItem.assetHeight / messageItem.assetWidth
                }
            } else {
                if (dp64 * messageItem.assetWidth / messageItem.assetHeight > dp120) {
                    binding.chatSticker.layoutParams.height = dp120 * messageItem.assetHeight / messageItem.assetWidth
                    binding.chatSticker.layoutParams.width = dp120
                } else {
                    binding.chatSticker.layoutParams.height = dp64
                    binding.chatSticker.layoutParams.width = dp64 * messageItem.assetWidth / messageItem.assetHeight
                }
            }
            binding.chatTime.visibility = VISIBLE
        } else if (messageItem.assetWidth * 2 > dp120 || messageItem.assetHeight * 2 > dp120) {
            if (messageItem.assetWidth > messageItem.assetHeight) {
                binding.chatSticker.layoutParams.width = dp120
                binding.chatSticker.layoutParams.height = dp120 * messageItem.assetHeight / messageItem.assetWidth
            } else {
                binding.chatSticker.layoutParams.height = dp120
                binding.chatSticker.layoutParams.width = dp120 * messageItem.assetWidth / messageItem.assetHeight
            }
            binding.chatTime.visibility = VISIBLE
        } else {
            binding.chatSticker.layoutParams.width = messageItem.assetWidth * 2
            binding.chatSticker.layoutParams.height = messageItem.assetHeight * 2
            binding.chatTime.visibility = VISIBLE
        }
        messageItem.assetUrl?.let { url ->
            binding.chatSticker.loadSticker(url, messageItem.assetType, "$url${messageItem.messageId}")
        }

        if (isFirst && !isMe) {
            binding.chatName.visibility = VISIBLE
            binding.chatName.setMessageName(messageItem)
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = GONE
        }

        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            MessageStatus.DELIVERED.name,
            false,
            isRepresentative = false,
            isSecret = false,
        )

        chatLayout(isMe, false)
        if (messageItem.transcriptId == null) {
            binding.root.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            binding.chatLayout.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            chatJumpLayout(binding.chatJump, isMe, messageItem.messageId, R.id.chat_layout, onItemListener)
        }
    }

    override fun chatLayout(
        isMe: Boolean,
        isLast: Boolean,
        isBlink: Boolean,
    ) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            itemView.requestLayout()
        } else {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            itemView.requestLayout()
        }
    }
}
