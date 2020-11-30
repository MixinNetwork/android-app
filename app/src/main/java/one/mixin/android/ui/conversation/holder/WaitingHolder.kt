package one.mixin.android.ui.conversation.holder

import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatWaitingBinding
import one.mixin.android.extension.highlightLinkText
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import org.jetbrains.anko.dip

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
        binding.chatTime.timeAgoClock(messageItem.createdAt)
        val learn: String = MixinApplication.get().getString(R.string.chat_learn)
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

        if (isFirst) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = itemView.dip(3)
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
