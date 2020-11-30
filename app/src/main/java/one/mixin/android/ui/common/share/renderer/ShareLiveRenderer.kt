package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatVideoBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.realSize
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus
import one.mixin.android.websocket.LiveMessagePayload

class ShareLiveRenderer(val context: Context) : ShareMessageRenderer {
    private val binding = ItemChatVideoBinding.inflate(LayoutInflater.from(context), null, false)
    val contentView get() = binding.root

    private val mediaWidth by lazy {
        (context.realSize().x * 0.6).toInt()
    }

    init {
        binding.chatName.visibility = View.GONE
        binding.chatWarning.visibility = View.GONE
        binding.durationTv.visibility = View.GONE
        binding.progress.visibility = View.GONE
        binding.play.isVisible = true
        binding.liveTv.visibility = View.VISIBLE
        (binding.liveTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 10.dp
        (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.CENTER
        (binding.durationTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
        (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
        binding.chatImage.setShape(R.drawable.chat_mark_image_me)
        (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 10.dp
        setStatusIcon(context, MessageStatus.DELIVERED.name, isSecret = true, isWhite = true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, 12.dp, 12.dp)
            secretIcon?.setBounds(0, 0, 8.dp, 8.dp)
            TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, secretIcon, null, statusIcon, null)
        }
    }

    fun render(data: LiveMessagePayload) {
        binding.chatImage.layoutParams.width = mediaWidth
        binding.chatImage.layoutParams.height = mediaWidth * data.height / data.width
        binding.chatImage.loadImageMark(data.thumbUrl, R.drawable.image_holder, R.drawable.chat_mark_image_me)
        binding.chatTime.timeAgoClock(nowInUtc())
    }
}
