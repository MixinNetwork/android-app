package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.LayoutInflater
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatTextBinding
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.MessageStatus
import one.mixin.android.widget.linktext.AutoLinkMode

open class ShareTextRenderer(val context: Context) {

    val binding = ItemChatTextBinding.inflate(LayoutInflater.from(context), null, false)
    val contentView get() = binding.root

    init {
        binding.chatTv.addAutoLinkMode(AutoLinkMode.MODE_URL)
    }

    fun render(content: String, isNightMode: Boolean) {
        binding.chatName.isVisible = false
        binding.chatTime.load(
            true,
            nowInUtc(),
            MessageStatus.DELIVERED.name,
            isPin = false,
            isRepresentative = false,
            isSecret = true
        )
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
