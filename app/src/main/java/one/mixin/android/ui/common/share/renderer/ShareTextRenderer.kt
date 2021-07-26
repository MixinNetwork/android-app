package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus
import one.mixin.android.widget.linktext.AutoLinkMode

open class ShareTextRenderer(val context: Context) : ShareMessageRenderer {

    val binding = ItemChatTextBinding.inflate(LayoutInflater.from(context), null, false)
    val contentView get() = binding.root

    init {
        binding.chatTv.addAutoLinkMode(
            AutoLinkMode.MODE_URL,
            AutoLinkMode.MODE_BOT,
            AutoLinkMode.MODE_MARKDOWN_BOLD,
            AutoLinkMode.MODE_MARKDOWN_ITALIC,
            AutoLinkMode.MODE_MARKDOWN_STRIKETHROUGH,
            AutoLinkMode.MODE_MARKDOWN_INLINE
        )
    }

    fun render(content: String, isNightMode: Boolean) {
        binding.chatName.isVisible = false
        binding.dataWrapper.chatTime.timeAgoClock(nowInUtc())
        setStatusIcon(context, MessageStatus.DELIVERED.name, isSecret = true, isWhite = false) { statusIcon, secretIcon ->
            binding.dataWrapper.chatFlag.isVisible = statusIcon != null
            binding.dataWrapper.chatFlag.setImageDrawable(statusIcon)
            binding.dataWrapper.chatSecret.isVisible = secretIcon != null
        }
        binding.chatTv.text = content
        binding.chatLayout.setBackgroundResource(
            if (!isNightMode) {
                R.drawable.chat_bubble_me_last
            } else {
                R.drawable.chat_bubble_me_last_night
            }
        )
    }
}
