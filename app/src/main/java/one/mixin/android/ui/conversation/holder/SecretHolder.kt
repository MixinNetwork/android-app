package one.mixin.android.ui.conversation.holder

import android.content.res.ColorStateList
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
                    if (isGroup) R.string.group_disappearing_message_enabled_hint else R.string.chat_disappearing_message_enabled_hint,
                    toTimeInterval(expireIn),
                )
            } else {
                context.getString(
                    if (isGroup) R.string.group_disappearing_message_disabled_hint else R.string.chat_disappearing_message_disabled_hint,
                )
            }
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    context,
                    if (enabled) {
                        R.color.disappearing_message_background
                    } else if (isNightMode) {
                        R.color.bgGrayNight
                    } else {
                        R.color.disappearing_message_disabled_background
                    },
                ),
            )
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
