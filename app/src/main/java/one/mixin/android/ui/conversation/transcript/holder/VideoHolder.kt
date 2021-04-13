package one.mixin.android.ui.conversation.transcript.holder

import android.view.Gravity
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatVideoBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadImageMark
import one.mixin.android.extension.loadVideoMark
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.holder.MediaHolder
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isLive
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip

class VideoHolder constructor(val binding: ItemChatVideoBinding) : MediaHolder(binding.root) {

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        binding.chatImage.round(radius)
        binding.chatTime.round(radius)
        binding.progress.round(radius)
    }

    private val dp4 by lazy {
        itemView.context.dpToPx(4f)
    }

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean,
        onClickListener: View.OnClickListener
    ) {
        super.bind(messageItem)

        val isMe = false
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

        if (messageItem.isLive()) {
            binding.chatWarning.visibility = GONE
            binding.durationTv.visibility = GONE
            binding.progress.visibility = GONE
            binding.play.isVisible = true
            binding.liveTv.visibility = VISIBLE
            binding.progress.setBindId(messageItem.messageId)
        } else {
            binding.liveTv.visibility = GONE

            binding.durationTv.bindId(null)
            messageItem.mediaDuration.notNullWithElse(
                {
                    binding.durationTv.visibility = VISIBLE
                    binding.durationTv.text = it.toLongOrNull()?.formatMillis() ?: ""
                },
                {
                    binding.durationTv.visibility = GONE
                }
            )
            binding.chatWarning.visibility = GONE
            binding.progress.visibility = GONE
            binding.play.visibility = VISIBLE
            binding.progress.setBindId(messageItem.messageId)
            binding.progress.setOnClickListener {}
        }

        binding.chatTime.timeAgoClock(messageItem.createdAt)

        setStatusIcon(
            isMe,
            messageItem.status,
            messageItem.isSignal(),
            isRepresentative = false,
            isWhite = true
        ) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, secretIcon ?: representativeIcon, null, statusIcon, null)
        }

        dataWidth = messageItem.mediaWidth
        dataHeight = messageItem.mediaHeight
        dataUrl = if (messageItem.isLive()) {
            messageItem.thumbUrl
        } else {
            messageItem.mediaUrl
        }
        type = messageItem.type
        dataThumbImage = messageItem.thumbImage
        binding.chatImageLayout.setOnClickListener(onClickListener)
        chatLayout(isMe, isLast)
    }

    private var dataUrl: String? = null
    private var type: String? = null
    private var dataThumbImage: String? = null
    private var dataWidth: Int? = null
    private var dataHeight: Int? = null

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.END
            (binding.chatImageLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
            (binding.durationTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp4
            (binding.liveTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp4
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp10
        } else {
            (binding.chatLayout.layoutParams as FrameLayout.LayoutParams).gravity = Gravity.START
            (binding.chatImageLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
            (binding.durationTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp10
            (binding.liveTv.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp10
            (binding.chatTime.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp3
        }

        var width = mediaWidth - dp6
        when {
            isLast -> {
                width = mediaWidth
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
            }
            isMe -> {
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = dp6
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginStart = 0
            }
            else -> {
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginEnd = 0
                (binding.chatImage.layoutParams as ViewGroup.MarginLayoutParams).marginStart = dp6
            }
        }
        if (dataWidth == null || dataHeight == null ||
            dataWidth!! <= 0 || dataHeight!! <= 0
        ) {
            binding.chatImage.layoutParams.width = width
            binding.chatImage.layoutParams.height = width
        } else {
            binding.chatImage.layoutParams.width = width
            binding.chatImage.layoutParams.height = width * dataHeight!! / dataWidth!!
        }

        val mark = when {
            isMe && isLast -> R.drawable.chat_mark_image_me
            isMe -> R.drawable.chat_mark_image
            !isMe && isLast -> R.drawable.chat_mark_image_other
            else -> R.drawable.chat_mark_image
        }

        binding.chatImage.setShape(mark)
        if (type == MessageCategory.PLAIN_LIVE.name || type == MessageCategory.SIGNAL_LIVE.name) {
            binding.chatImage.loadImageMark(dataUrl, R.drawable.image_holder, mark)
        } else {
            binding.chatImage.loadVideoMark(dataUrl, dataThumbImage, mark)
        }
    }
}
