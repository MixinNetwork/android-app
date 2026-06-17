package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatContactCardQuoteBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.round
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.MediaHolder
import one.mixin.android.ui.conversation.holder.base.Terminable
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret

class ContactCardQuoteHolder constructor(val binding: ItemChatContactCardQuoteBinding) :
    MediaHolder(binding.root),
    Terminable {
        init {
            val radius = itemView.context.dpToPx(4f).toFloat()
            binding.chatTime.round(radius)
        }

        override fun chatLayout(
            isMe: Boolean,
            isLast: Boolean,
            isBlink: Boolean,
        ) {
            super.chatLayout(isMe, isLast, isBlink)
            if (isMe) {
                (binding.chatMsgLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
                if (isLast) {
                    setItemBackgroundResource(
                        binding.chatLayout,
                        R.drawable.chat_bubble_reply_me_last,
                        R.drawable.chat_bubble_reply_me_last_night,
                    )
                } else {
                    setItemBackgroundResource(
                        binding.chatLayout,
                        R.drawable.chat_bubble_reply_me,
                        R.drawable.chat_bubble_reply_me_night,
                    )
                }
            } else {
                (binding.chatMsgLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
                if (isLast) {
                    setItemBackgroundResource(
                        binding.chatLayout,
                        R.drawable.chat_bubble_reply_other_last,
                        R.drawable.chat_bubble_reply_other_last_night,
                    )
                } else {
                    setItemBackgroundResource(
                        binding.chatLayout,
                        R.drawable.chat_bubble_reply_other,
                        R.drawable.chat_bubble_reply_other_night,
                    )
                }
            }
        }

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
            binding.idTv.text = messageItem.sharedUserIdentityNumber
            binding.nameTv.setName(messageItem)

            val isMe = Session.getAccountId() == messageItem.userId
            if (isFirst && !isMe) {
                binding.chatName.visibility = View.VISIBLE
                binding.chatName.setMessageName(messageItem)
                binding.chatName.setTextColor(getColorById(messageItem.userId))
                binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
            } else {
                binding.chatName.visibility = View.GONE
            }

            chatLayout(isMe, isLast)

            binding.chatLayout.setOnClickListener {
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
            binding.chatLayout.setOnLongClickListener {
                if (!hasSelect) {
                    onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                } else {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                    true
                }
            }
            binding.chatQuote.bind(fromJsonQuoteMessage(messageItem.quoteContent))
            binding.chatQuote.setOnClickListener {
                if (!hasSelect) {
                    onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
                } else {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                }
            }
            binding.chatTime.load(
                isMe,
                messageItem.createdAt,
                messageItem.status,
                messageItem.isPin ?: false,
                isRepresentative = isRepresentative,
                isSecret = messageItem.isSecret(),
            )
            chatJumpLayout(binding.chatJump, isMe, messageItem.expireIn, messageItem.expireAt, R.id.chat_msg_layout)
        }
    }
