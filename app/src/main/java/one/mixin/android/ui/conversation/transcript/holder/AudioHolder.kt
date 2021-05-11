package one.mixin.android.ui.conversation.transcript.holder

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatAudioBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.conversation.transcript.TranscriptAdapter
import one.mixin.android.util.AudioPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.TranscriptMessageItem
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.mediaDownloaded
import org.jetbrains.anko.dip
import org.jetbrains.anko.textResource
import kotlin.math.min

class AudioHolder constructor(val binding: ItemChatAudioBinding) : BaseViewHolder(binding.root) {
    init {
        binding.billTime.chatFlag.visibility = View.GONE
    }

    private val maxWidth by lazy {
        itemView.context.dpToPx(255f)
    }

    private val minWidth by lazy {
        itemView.context.dpToPx(180f)
    }

    private val dp15 by lazy {
        itemView.context.dpToPx(15f)
    }

    fun bind(
        messageItem: TranscriptMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: TranscriptAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        chatLayout(isMe, isLast)
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = itemView.dip(3)
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }
        binding.billTime.chatTime.timeAgoClock(messageItem.createdAt)

        if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
            binding.audioDuration.textResource = R.string.chat_expired
        } else {
            binding.audioDuration.text = messageItem.mediaDuration?.toLongOrNull()?.formatMillis() ?: "00:00"
        }

        messageItem.mediaDuration?.let {
            val duration = try {
                it.toLong()
            } catch (e: Exception) {
                0L
            }
            binding.chatLayout.layoutParams.width =
                min((minWidth + (duration / 1000f) * dp15).toInt(), maxWidth)
        }
        setStatusIcon(isMe, MessageStatus.DELIVERED.name, messageItem.isSignal(), false) { statusIcon, secretIcon, representativeIcon ->
            binding.billTime.chatFlag.isVisible = statusIcon != null
            binding.billTime.chatFlag.setImageDrawable(statusIcon)
            binding.billTime.chatSecret.isVisible = secretIcon != null
            binding.billTime.chatRepresentative.isVisible = representativeIcon != null
        }

        messageItem.mediaWaveform?.let {
            binding.audioWaveform.setWaveform(it)
        }
        if (!isMe && messageItem.mediaStatus != MediaStatus.READ.name) {
            binding.audioDuration.setTextColor(itemView.context.getColor(R.color.colorBlue))
            binding.audioWaveform.isFresh = true
        } else {
            binding.audioDuration.setTextColor(itemView.context.getColor(R.color.gray_50))
            binding.audioWaveform.isFresh = false
        }
        if (AudioPlayer.isLoaded(messageItem.messageId)) {
            binding.audioWaveform.setProgress(AudioPlayer.getProgress())
        } else {
            binding.audioWaveform.setProgress(0f)
        }
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    binding.audioExpired.visibility = View.VISIBLE
                    binding.audioProgress.visibility = View.INVISIBLE
                    binding.chatLayout.setOnClickListener {
                    }
                }
                MediaStatus.PENDING.name -> {
                    binding.audioExpired.visibility = View.GONE
                    binding.audioProgress.visibility = View.VISIBLE
                    binding.audioProgress.enableLoading(MixinJobManager.getAttachmentProcess(messageItem.messageId))
                    binding.audioProgress.setBindOnly(messageItem.messageId)
                    binding.audioProgress.setOnClickListener {
                    }
                    binding.chatLayout.setOnClickListener {
                    }
                }
                MediaStatus.DONE.name, MediaStatus.READ.name -> {
                    binding.audioExpired.visibility = View.GONE
                    binding.audioProgress.visibility = View.VISIBLE
                    binding.audioProgress.setBindOnly(messageItem.messageId)
                    binding.audioWaveform.setBind(messageItem.messageId)
                    if (AudioPlayer.isPlay(messageItem.messageId)) {
                        binding.audioProgress.setPause()
                    } else {
                        binding.audioProgress.setPlay()
                    }
                    binding.audioProgress.setOnClickListener {
                    }
                    binding.chatLayout.setOnClickListener {
                    }
                }
                MediaStatus.CANCELED.name -> {
                    binding.audioExpired.visibility = View.GONE
                    binding.audioProgress.visibility = View.VISIBLE
                    if (isMe) {
                        binding.audioProgress.enableUpload()
                    } else {
                        binding.audioProgress.enableDownload()
                    }
                    binding.audioProgress.setBindOnly(messageItem.messageId)
                    binding.audioProgress.setProgress(-1)
                    binding.audioProgress.setOnClickListener {
                        if (isMe) {
                            onItemListener.onRetryUpload(messageItem.messageId)
                        } else {
                            onItemListener.onRetryDownload(messageItem.messageId)
                        }
                    }
                    binding.chatLayout.setOnClickListener {
                    }
                }
            }
        }
    }

    private fun handleClick(
        messageItem: TranscriptMessageItem,
        onItemListener: TranscriptAdapter.OnItemListener
    ) {
        if (messageItem.mediaStatus == MediaStatus.CANCELED.name) {
            if (messageItem.mediaUrl.isNullOrEmpty()) {
                onItemListener.onRetryDownload(messageItem.messageId)
            } else {
                onItemListener.onRetryUpload(messageItem.messageId)
            }
        } else if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
            onItemListener.onCancel(messageItem.messageId)
        } else if (messageItem.mediaStatus != MediaStatus.EXPIRED.name) {
            onItemListener.onAudioClick(messageItem)
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.bill_bubble_me_last,
                    R.drawable.bill_bubble_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.bill_bubble_me,
                    R.drawable.bill_bubble_me_night
                )
            }
            (binding.chatLayout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
        } else {
            (binding.chatLayout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night
                )
            }
        }
    }
}
