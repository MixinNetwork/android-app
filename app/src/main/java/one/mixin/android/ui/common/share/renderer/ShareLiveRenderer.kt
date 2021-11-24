package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatVideoBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.realSize
import one.mixin.android.vo.MessageStatus
import one.mixin.android.websocket.LiveMessagePayload

class ShareLiveRenderer(val context: Context) {
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
        (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0.5f
        (binding.durationTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
        (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
        binding.chatImage.setShape(R.drawable.chat_mark_image_me)
        (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 10.dp
    }

    fun render(data: LiveMessagePayload) {
        binding.chatImage.layoutParams.width = mediaWidth
        binding.chatImage.layoutParams.height = mediaWidth * data.height / data.width
        binding.chatImage.loadImageMark(data.thumbUrl, R.drawable.image_holder, R.drawable.chat_mark_image_me)
        binding.chatTime.load(
            true,
            nowInUtc(),
            MessageStatus.DELIVERED.name,
            isPin = false,
            isRepresentative = false,
            isSecret = true,
            isWhite = true
        )
    }
}
