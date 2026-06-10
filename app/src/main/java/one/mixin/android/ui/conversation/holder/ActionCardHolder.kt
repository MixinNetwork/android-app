package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatActionCardBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadRoundImage
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret

class ActionCardHolder(val binding: ItemChatActionCardBinding) :
    BaseViewHolder(binding.root) {
        private val radius by lazy {
            4.dp
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
            val isMe = meId == messageItem.userId
            chatLayout(isMe, isLast)
            if (hasSelect && isSelect) {
                itemView.setBackgroundColor(SELECT_COLOR)
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }
            val longClickListener =
                View.OnLongClickListener {
                    if (!hasSelect) {
                        onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
                    } else {
                        onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                        true
                    }
                }
            itemView.setOnLongClickListener(longClickListener)
            binding.chatLayout.setOnLongClickListener(longClickListener)

            binding.chatTime.load(
                isMe,
                messageItem.createdAt,
                messageItem.status,
                messageItem.isPin ?: false,
                isRepresentative = isRepresentative,
                isSecret = messageItem.isSecret(),
            )

            if (isFirst && !isMe) {
                binding.chatName.visibility = View.VISIBLE
                binding.chatName.setMessageName(messageItem)
                binding.chatName.setTextColor(getColorById(messageItem.userId))
                binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
            } else {
                binding.chatName.visibility = View.GONE
            }
            val actionCard =
                GsonHelper.customGson.fromJson(messageItem.content, AppCardData::class.java)
            binding.chatIcon.loadRoundImage(actionCard.iconUrl, radius, R.drawable.holder_bot)
            binding.chatTitle.text = actionCard.title
            binding.chatDescription.text = actionCard.description
            binding.chatLayout.setOnClickListener {
                if (hasSelect) {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                } else {
                    onItemListener.onAppCardClick(actionCard, messageItem.userId)
                }
            }
            itemView.setOnClickListener {
                if (hasSelect) {
                    onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                }
            }
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
