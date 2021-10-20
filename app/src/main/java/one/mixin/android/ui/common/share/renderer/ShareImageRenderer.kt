package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatImageBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.nowInUtc
import one.mixin.android.extension.realSize
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.ShareImageData

class ShareImageRenderer(val context: Context) : ShareMessageRenderer {

    private val binding = ItemChatImageBinding.inflate(LayoutInflater.from(context), null, false)
    val contentView get() = binding.root

    private val mediaWidth by lazy {
        (context.realSize().x * 0.6).toInt()
    }

    init {
        binding.chatImage.setShape(R.drawable.chat_mark_image_me)
        binding.chatName.visibility = View.GONE
        binding.chatWarning.visibility = View.GONE
        binding.progress.visibility = View.GONE
        (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0.5f
        (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
        (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 10.dp
        binding.chatImage.layoutParams.width = mediaWidth
        binding.chatImage.adjustViewBounds = true
        binding.chatImage.scaleType = ImageView.ScaleType.FIT_XY
        setStatusIcon(context, MessageStatus.DELIVERED.name, isSecret = true, isWhite = true) { statusIcon, secretIcon ->
            statusIcon?.setBounds(0, 0, 12.dp, 12.dp)
            secretIcon?.setBounds(0, 0, 8.dp, 8.dp)
            binding.chatTime.setIcon(secretIcon, null, statusIcon)
        }
    }

    fun render(data: ShareImageData) {
        binding.chatImage.loadImageMark(data.url, R.drawable.image_holder, R.drawable.chat_mark_image_me)
        binding.chatTime.timeAgoClock(nowInUtc())
    }
}
