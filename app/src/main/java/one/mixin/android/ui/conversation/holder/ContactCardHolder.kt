package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatContactCardBinding
import one.mixin.android.extension.dp
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.ui.conversation.holder.base.Terminable
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret

class ContactCardHolder(val binding: ItemChatContactCardBinding) : BaseViewHolder(binding.root), Terminable {
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

        binding.avatarIv.setInfo(
            messageItem.sharedUserFullName,
            messageItem.sharedUserAvatarUrl,
            messageItem.sharedUserId
                ?: "0",
        )
        binding.nameTv.setName(messageItem)
        binding.idTv.text = messageItem.sharedUserIdentityNumber

        val isMe = Session.getAccountId() == messageItem.userId
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

        chatLayout(isMe, isLast)

        binding.chatContentLayout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onContactCardClick(messageItem.sharedUserId!!)
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
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.bill_bubble_me_last,
                    R.drawable.bill_bubble_me_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.bill_bubble_me,
                    R.drawable.bill_bubble_me_night,
                )
            }
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 16.dp
        } else {
            (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 8.dp
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night,
                )
            } else {
                setItemBackgroundResource(
                    binding.chatContentLayout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night,
                )
            }
        }
    }
}
