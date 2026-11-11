package one.mixin.android.ui.conversation.holder

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatSecretBinding
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.widget.picker.toTimeInterval

class SecretHolder constructor(val binding: ItemChatSecretBinding) : BaseViewHolder(binding.root) {
    fun bind(
        expireIn: Long,
        isSecret: Boolean,
        isGroup: Boolean,
        canEdit: Boolean,
        hasSelect: Boolean,
        onItemListener: MessageAdapter.OnItemListener,
    ) {
        binding.chatInfo.isVisible = isSecret
        if (isSecret) {
            binding.chatInfo.setOnClickListener {
                onItemListener.onUrlClick(itemView.context.getString(R.string.secret_url))
            }
        } else {
            binding.chatInfo.setOnClickListener(null)
            binding.chatInfo.isClickable = false
        }
        val enabled = expireIn > 0L
        binding.disappearingInfo.apply {
            text = if (enabled) {
                context.getString(
                    if (isGroup && !canEdit) R.string.group_disappearing_message_enabled_read_only else if (isGroup) R.string.group_disappearing_message_enabled_editable else R.string.chat_disappearing_message_enabled_editable,
                    toTimeInterval(expireIn),
                )
            } else {
                context.getString(
                    if (isGroup && !canEdit) R.string.group_disappearing_message_disabled_read_only else if (isGroup) R.string.group_disappearing_message_disabled_editable else R.string.chat_disappearing_message_disabled_editable,
                )
            }
            val start = paddingStart
            val top = paddingTop
            val end = paddingEnd
            val bottom = paddingBottom
            setBackgroundResource(
                if (enabled) {
                    R.drawable.chat_bubble_disappearing
                } else if (isNightMode) {
                    R.drawable.chat_bubble_disappearing_off_night
                } else {
                    R.drawable.chat_bubble_disappearing_off
                },
            )
            setPaddingRelative(start, top, end, bottom)
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isNightMode && !enabled) R.color.textPrimaryNight else R.color.textPrimary,
                ),
            )
            setOnClickListener {
                if (!hasSelect) onItemListener.onDisappearingMessagesClick()
            }
        }
    }
}
