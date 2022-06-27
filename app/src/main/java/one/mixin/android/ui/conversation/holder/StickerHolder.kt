package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.databinding.ItemChatStickerBinding
import one.mixin.android.event.ExpiredEvent
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadSticker
import one.mixin.android.extension.round
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.ui.conversation.holder.base.Terminable
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret

class StickerHolder constructor(val binding: ItemChatStickerBinding) :
    BaseViewHolder(binding.root),
    Terminable {

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
        messageItem: MessageItem,
        isFirst: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        val isMe = meId == messageItem.userId
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        val longClickListener = View.OnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        itemView.setOnLongClickListener(longClickListener)
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }
        binding.chatSticker.setOnClickListener {
            onItemListener.onStickerClick(messageItem)
        }
        binding.chatSticker.setOnLongClickListener(longClickListener)
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
            binding.chatName.visibility = GONE
        }
        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            messageItem.status,
            messageItem.isPin ?: false,
            isRepresentative = isRepresentative,
            isSecret = messageItem.isSecret()
        )
        chatJumpLayout(binding.chatJump, isMe, messageItem.expireIn, R.id.chat_layout)
        chatLayout(isMe, false)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            itemView.requestLayout()
        } else {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            itemView.requestLayout()
        }
    }

    override fun onRead(messageItem: MessageItem) {
        if (messageItem.expireIn != null) {
            RxBus.publish(ExpiredEvent(messageItem.messageId, messageItem.expireIn))
        }
    }
}
