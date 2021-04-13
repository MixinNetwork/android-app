package one.mixin.android.ui.conversation.transcript.holder

import android.view.Gravity
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import androidx.core.widget.TextViewCompat
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatStickerBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.loadSticker
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.util.image.ImageListener
import one.mixin.android.util.image.LottieLoader
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isLottie
import one.mixin.android.vo.isSignal
import one.mixin.android.widget.RLottieDrawable
import org.jetbrains.anko.dip
import org.jetbrains.anko.textColorResource

class StickerHolder constructor(val binding: ItemChatStickerBinding) : BaseViewHolder(binding.root) {

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        binding.chatTime.textColorResource = R.color.color_chat_date
        binding.chatSticker.round(radius)
    }

    private val dp120 by lazy {
        itemView.context.dpToPx(120f)
    }

    private val dp64 by lazy {
        itemView.context.dpToPx(64f)
    }

    fun bind(
        messageItem: MessageItem,
        isFirst: Boolean,
    ) {
        super.bind(messageItem)
        val isMe = false
        if (messageItem.assetWidth == null || messageItem.assetHeight == null) {
            binding.chatSticker.layoutParams.width = dp120
            binding.chatSticker.layoutParams.height = dp120
            binding.chatTime.visibility = INVISIBLE
        } else if (messageItem.assetWidth * 2 < dp64 || messageItem.assetHeight * 2 < dp64) {
            if (messageItem.assetWidth < messageItem.assetHeight) {
                if (dp64 * messageItem.assetHeight / messageItem.assetWidth > dp120) {
                    binding.chatSticker.layoutParams.width = dp120 * messageItem.assetWidth / messageItem.assetHeight
                    binding.chatSticker.layoutParams.height = dp120
                } else {
                    binding.chatSticker.layoutParams.width = dp64
                    binding.chatSticker.layoutParams.height = dp64 * messageItem.assetHeight / messageItem.assetWidth
                }
            } else {
                if (dp64 * messageItem.assetWidth / messageItem.assetHeight > dp120) {
                    binding.chatSticker.layoutParams.height = dp120 * messageItem.assetHeight / messageItem.assetWidth
                    binding.chatSticker.layoutParams.width = dp120
                } else {
                    binding.chatSticker.layoutParams.height = dp64
                    binding.chatSticker.layoutParams.width = dp64 * messageItem.assetWidth / messageItem.assetHeight
                }
            }
            binding.chatTime.visibility = VISIBLE
        } else if (messageItem.assetWidth * 2 > dp120 || messageItem.assetHeight * 2 > dp120) {
            if (messageItem.assetWidth > messageItem.assetHeight) {
                binding.chatSticker.layoutParams.width = dp120
                binding.chatSticker.layoutParams.height = dp120 * messageItem.assetHeight / messageItem.assetWidth
            } else {
                binding.chatSticker.layoutParams.height = dp120
                binding.chatSticker.layoutParams.width = dp120 * messageItem.assetWidth / messageItem.assetHeight
            }
            binding.chatTime.visibility = VISIBLE
        } else {
            binding.chatSticker.layoutParams.width = messageItem.assetWidth * 2
            binding.chatSticker.layoutParams.height = messageItem.assetHeight * 2
            binding.chatTime.visibility = VISIBLE
        }
        messageItem.assetUrl?.let { url ->
            if (messageItem.isLottie()) {
                LottieLoader.fromUrl(
                    itemView.context,
                    url,
                    url,
                    binding.chatSticker.layoutParams.width,
                    binding.chatSticker.layoutParams.height
                )
                    .addListener(
                        object : ImageListener<RLottieDrawable> {
                            override fun onResult(result: RLottieDrawable) {
                                binding.chatSticker.setAnimation(result)
                                binding.chatSticker.playAnimation()
                                binding.chatSticker.setAutoRepeat(true)
                            }
                        }
                    )
            } else {
                binding.chatSticker.loadSticker(url, messageItem.assetType)
            }
        }
        binding.chatTime.timeAgoClock(messageItem.createdAt)
        if (isFirst && !isMe) {
            binding.chatName.visibility = VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = itemView.dip(3)
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
        } else {
            binding.chatName.visibility = GONE
        }
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), false) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, secretIcon ?: representativeIcon, null, statusIcon, null)
        }
        chatLayout(isMe, false)
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
            itemView.requestLayout()
        } else {
            (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            itemView.requestLayout()
        }
    }
}
