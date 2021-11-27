package one.mixin.android.ui.conversation.chathistory.holder

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatRecallBinding
import one.mixin.android.ui.conversation.chathistory.ChatHistoryAdapter
import one.mixin.android.vo.ChatHistoryMessageItem
import org.jetbrains.anko.dip

class RecallHolder constructor(val binding: ItemChatRecallBinding) : BaseViewHolder(binding.root) {

    fun bind(
        messageItem: ChatHistoryMessageItem,
        isFirst: Boolean,
        isLast: Boolean,
        onItemListener: ChatHistoryAdapter.OnItemListener
    ) {
        val ctx = itemView.context
        val isMe = meId == messageItem.userId
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
            binding.chatName.setOnClickListener {
                messageItem.userId?.let { userId ->
                    onItemListener.onUserClick(userId)
                }
            }
        } else {
            binding.chatName.visibility = View.GONE
        }

        if (messageItem.appId != null) {
            binding.chatName.setCompoundDrawables(null, null, botIcon, null)
            binding.chatName.compoundDrawablePadding = itemView.dip(3)
        } else {
            binding.chatName.setCompoundDrawables(null, null, null, null)
        }
        chatLayout(isMe, isLast)
        binding.chatTime.load(messageItem.createdAt)
        binding.recallTv.text = if (isMe) {
            ctx.getString(R.string.chat_recall_me) + " "
        } else {
            ctx.getString(R.string.chat_recall_delete) + " "
        }
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
}
