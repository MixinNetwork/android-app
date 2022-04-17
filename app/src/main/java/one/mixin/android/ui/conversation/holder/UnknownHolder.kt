package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatUnknownBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.vo.MessageItem

class UnknownHolder constructor(val binding: ItemChatUnknownBinding) : BaseViewHolder(binding.root) {

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
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
            (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
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

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
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

        val isMe = meId == messageItem.userId

        val learn: String = MixinApplication.get().getString(R.string.Learn_more)
        val info = MixinApplication.get().getString(R.string.chat_not_support, learn)
        val learnUrl = MixinApplication.get().getString(R.string.chat_not_support_url)
        binding.chatTv.highlightLinkText(
            info,
            arrayOf(learn),
            arrayOf(learnUrl),
            onItemListener = onItemListener
        )

        if (isFirst) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = 3.dp
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
        } else {
            binding.chatName.visibility = View.GONE
        }
        chatLayout(isMe, isLast)
    }
}
