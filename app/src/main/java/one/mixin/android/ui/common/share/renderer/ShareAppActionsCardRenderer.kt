package one.mixin.android.ui.common.share.renderer

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatActionsCardBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.nowInUtc
import one.mixin.android.ui.conversation.holder.AppCard
import one.mixin.android.util.ColorUtil
import one.mixin.android.vo.AppCardData
import one.mixin.android.vo.MessageStatus
import one.mixin.android.widget.ActionButton

open class ShareAppActionsCardRenderer(val context: Context, containerWidth: Int) {
    private val binding = ItemChatActionsCardBinding.inflate(LayoutInflater.from(context), null, false)
    val contentView get() = ScrollView(context).apply {
            addView(binding.root, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        }

    val width = containerWidth

    init {
        (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0.5f
        (binding.chatLayout.layoutParams as ConstraintLayout.LayoutParams).width = width
        binding.chatGroupLayout.setLineSpacing(6.dp)
        (binding.chatGroupLayout.layoutParams as LinearLayout.LayoutParams).apply {
            width = this@ShareAppActionsCardRenderer.width - 16.dp
            marginStart = 4.dp
        }
        binding.chatName.isVisible = false
    }

    fun render(
        actionCard: AppCardData,
        isNightMode: Boolean,
    ) {
        // binding.chatContentLayout.setContent {
        //     AppCard(
        //         actionCard,
        //         contentClick = {
        //         },
        //         contentLongClick = {
        //         },
        //         urlClick = { _ ->
        //         },
        //         urlLongClick = { _ ->
        //         },
        //         width = width, createdAt = nowInUtc(), isLast = true, isMe = true,
        //         status = MessageStatus.DELIVERED.name,
        //         isPin = false,
        //         isRepresentative = false,
        //         isSecret = false,
        //     )
        // }
        if (!actionCard.actions.isNullOrEmpty()) {
            binding.chatGroupLayout.isVisible = true
            for (b in actionCard.actions) {
                val button = ActionButton(context, b.externalLink, b.sendLink)
                button.setTextColor(
                    try {
                        ColorUtil.parseColor(b.color.trim())
                    } catch (e: Throwable) {
                        Color.BLACK
                    },
                )
                button.setTypeface(null, Typeface.BOLD)
                button.setText(b.label)
                binding.chatGroupLayout.addView(button)
            }
        } else {
            binding.chatGroupLayout.isVisible = false
        }
        binding.chatContentLayout.setBackgroundResource(
            if (!isNightMode) {
                R.drawable.chat_bubble_post_me_last
            } else {
                R.drawable.chat_bubble_post_me_last_night
            },
        )
    }
}
