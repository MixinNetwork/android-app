package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.databinding.ItemChatStickerBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadSticker
import one.mixin.android.extension.nowInUtc
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.Sticker

class ShareStickerRenderer(val context: Context) {

    private val binding = ItemChatStickerBinding.inflate(LayoutInflater.from(context), null, false)
    val contentView get() = binding.root

    private val dp120 by lazy {
        context.dpToPx(120f)
    }

    init {
        binding.chatName.visibility = View.GONE
        (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0.5f
        (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
        (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 10.dp
        binding.chatSticker.layoutParams.width = dp120
    }

    fun render(sticker: Sticker) {
        binding.chatSticker.loadSticker(sticker.assetUrl, sticker.assetType, sticker.assetUrl)
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
