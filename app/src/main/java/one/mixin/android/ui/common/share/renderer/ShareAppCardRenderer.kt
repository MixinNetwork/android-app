package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatActionCardBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.loadRoundImage
import one.mixin.android.vo.AppCardData

open class ShareAppCardRenderer(context: Context) : ShareMessageRenderer {

    private val binding = ItemChatActionCardBinding.inflate(LayoutInflater.from(context), null, false)
    val contentView get() = binding.root

    init {
        (binding.chatLayout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER
        binding.chatName.isVisible = false
    }

    fun render(actionCard: AppCardData, isNightMode: Boolean) {
        binding.chatIcon.loadRoundImage(actionCard.iconUrl, 4.dp, R.drawable.holder_bot)
        binding.chatTitle.text = actionCard.title
        binding.chatDescription.text = actionCard.description
        binding.chatLayout.setBackgroundResource(
            if (!isNightMode) {
                R.drawable.bill_bubble_me_last
            } else {
                R.drawable.bill_bubble_me_last_night
            }
        )
    }
}
