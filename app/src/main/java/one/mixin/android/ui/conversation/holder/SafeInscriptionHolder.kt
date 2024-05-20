package one.mixin.android.ui.conversation.holder

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatSafeInscriptionBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadHexagonImage
import one.mixin.android.extension.loadImage
import one.mixin.android.extension.roundLeftOrRight
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret
import one.mixin.android.vo.safe.SafeInscription

class SafeInscriptionHolder(val binding: ItemChatSafeInscriptionBinding) : BaseViewHolder(binding.root) {
    init {
        binding.chatInscriptionIv.roundLeftOrRight(20f, true, false)
    }

    @SuppressLint("SetTextI18n")
    fun bind(
        messageItem: MessageItem,
        isFirst: Boolean,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: MessageAdapter.OnItemListener,
    ) {
        super.bind(messageItem)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        val isMe = Session.getAccountId() == messageItem.userId
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
        val safeInscription =
            try {
                GsonHelper.customGson.fromJson(messageItem.content, SafeInscription::class.java)
            } catch (e: Exception) {
                null
            }
        if (safeInscription != null) {
            binding.chatTitleTv.text = safeInscription.name
            binding.chatNumberTv.text = "#${safeInscription.sequence}"
            binding.chatInscriptionIv.loadImage(safeInscription.contentURL, R.drawable.ic_default_inscription)
            binding.chatInscriptionIcon.loadHexagonImage(safeInscription.iconURL, R.drawable.ic_inscription_icon)
            binding.chatBarcode.setData(safeInscription.inscriptionHash)
        }

        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            messageItem.status,
            messageItem.isPin ?: false,
            isRepresentative = isRepresentative,
            isSecret = messageItem.isSecret(),
        )

        chatLayout(isMe, isLast)

        binding.chatContentLayout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onInscriptionClick(messageItem.conversationId, messageItem.messageId, messageItem.assetId, safeInscription?.inscriptionHash, messageItem.snapshotId)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }
        itemView.setOnClickListener {
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
        binding.chatContentLayout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        chatJumpLayout(binding.chatJump, isMe, messageItem.expireIn, messageItem.expireAt, R.id.chat_layout)
    }

    override fun chatLayout(
        isMe: Boolean,
        isLast: Boolean,
        isBlink: Boolean,
    ) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 12.dp
        } else {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 6.dp
        }

        setItemBackgroundResource(
            binding.chatContentLayout,
            R.drawable.chat_bubble_post_me,
            R.drawable.chat_bubble_post_me_night,
        )
    }
}
