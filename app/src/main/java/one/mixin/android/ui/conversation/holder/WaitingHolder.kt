package one.mixin.android.ui.conversation.holder

import android.view.Gravity
import android.widget.FrameLayout
import androidx.core.view.isVisible
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatWaitingBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal

class WaitingHolder constructor(
    val binding: ItemChatWaitingBinding,
    private val onItemListener: ConversationAdapter.OnItemListener
) : BaseViewHolder(binding.root) {

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
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        val isMe = meId == messageItem.userId
        if (messageItem.isSignal()) {
            val learn: String = MixinApplication.get().getString(R.string.Learn_more)
            val info =
                MixinApplication.get().getString(
                    R.string.chat_waiting,
                    if (isMe) {
                        MixinApplication.get().getString(R.string.chat_waiting_desktop)
                    } else {
                        messageItem.userFullName
                    },
                    learn
                )
            val learnUrl = MixinApplication.get().getString(R.string.chat_waiting_url)
            binding.chatTv.highlightLinkText(
                info,
                arrayOf(learn),
                arrayOf(learnUrl),
                onItemListener = onItemListener
            )
        } else {
            binding.chatTv.setText(R.string.chat_decryption_failed)
        }
        if (isFirst) {
            binding.chatName.isVisible = !isMe
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
            binding.chatName.isVisible = false
        }
        chatLayout(isMe, isLast)
        binding.chatTime.chatStatus.isVisible = false
    }
}
