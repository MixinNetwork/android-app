package one.mixin.android.ui.conversation.chathistory.holder

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.core.view.isVisible
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatUnknownBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.highlightStarTag
import one.mixin.android.ui.conversation.chathistory.ChatHistoryAdapter
import one.mixin.android.vo.ChatHistoryMessageItem

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
        messageItem: ChatHistoryMessageItem,
        isLast: Boolean,
        isFirst: Boolean,
        onItemListener: ChatHistoryAdapter.OnItemListener
    ) {

        val learn: String = MixinApplication.get().getString(R.string.Learn_More)
        val info = MixinApplication.get().getString(R.string.chat_not_support, "**$learn**")
        val learnUrl = MixinApplication.get().getString(R.string.chat_not_support_url)
        binding.chatTv.highlightStarTag(
            info,
            arrayOf(learnUrl)
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
        binding.chatTime.chatStatus.isVisible = false
    }
}
