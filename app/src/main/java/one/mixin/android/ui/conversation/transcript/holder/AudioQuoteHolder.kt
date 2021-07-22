package one.mixin.android.ui.conversation.transcript.holder

import android.view.Gravity
import android.view.View
import androidx.core.widget.TextViewCompat
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatAudioQuoteBinding
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.transcript.TranscriptAdapter
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.SnakeQuoteMessageItem
import one.mixin.android.vo.TranscriptMessageItem
import org.jetbrains.anko.dip

class AudioQuoteHolder constructor(val binding: ItemChatAudioQuoteBinding) : MediaHolder(binding.root) {
    private val maxWidth by lazy {
        itemView.context.dpToPx(255f)
    }

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        binding.chatAudioLayout.round(radius)
        binding.chatTime.round(radius)
        binding.chatAudioLayout.layoutParams.width = maxWidth
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            binding.chatMsgLayout.gravity = Gravity.END
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night
                )
            }
        } else {
            binding.chatMsgLayout.gravity = Gravity.START
            if (isLast) {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    binding.chatLayout,
                    R.drawable.chat_bubble_reply_other,
                    R.drawable.chat_bubble_reply_other_night
                )
            }
        }
    }

    private var onItemListener: TranscriptAdapter.OnItemListener? = null

    fun bind(
        messageItem: TranscriptMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: TranscriptAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        this.onItemListener = onItemListener
        binding.chatTime.timeAgoClock(messageItem.createdAt)
        val isMe = messageItem.userId == Session.getAccountId()
        if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
            binding.audioDuration.setText(R.string.chat_expired)
        } else {
            binding.audioDuration.text = messageItem.mediaDuration?.toLongOrNull()?.formatMillis() ?: ""
        }
        setStatusIcon(isMe, MessageStatus.DELIVERED.name, isSecret = false, isRepresentative = false) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, secretIcon ?: representativeIcon, null, statusIcon, null)
        }

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
                    binding.audioProgress.setBindOnly("${messageItem.transcriptId}${messageItem.messageId}")
                    binding.audioProgress.setOnClickListener {
                        handleClick(messageItem, onItemListener)
                    }
                    binding.chatLayout.setOnClickListener {
                        handleClick(messageItem, onItemListener)
                    }
                }
                MediaStatus.DONE.name, MediaStatus.READ.name -> {
                    binding.audioExpired.visibility = View.GONE
                    binding.audioProgress.visibility = View.VISIBLE
                    binding.audioProgress.setBindOnly("${messageItem.transcriptId}${messageItem.messageId}")
                    binding.audioWaveform.setBind(messageItem.messageId)
                    if (AudioPlayer.isPlay(messageItem.messageId)) {
                        binding.audioProgress.setPause()
                    } else {
                        binding.audioProgress.setPlay()
                    }
                    binding.audioProgress.setOnClickListener {
                        handleClick(messageItem, onItemListener)
                    }
                    binding.chatLayout.setOnClickListener {
                        handleClick(messageItem, onItemListener)
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
                    binding.audioProgress.setBindOnly("${messageItem.transcriptId}${messageItem.messageId}")
                    binding.audioProgress.setProgress(-1)
                    binding.audioProgress.setOnClickListener {
                        handleClick(messageItem, onItemListener)
                    }
                    binding.chatLayout.setOnClickListener {
                        handleClick(messageItem, onItemListener)
                    }
                }
            }
        }

        val quoteMessage =
            GsonHelper.customGson.fromJson(messageItem.quoteContent, SnakeQuoteMessageItem::class.java)
        binding.chatQuote.bind(quoteMessage)
        binding.chatQuote.setOnClickListener {
            onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
        }
        chatLayout(isMe, isLast)
    }

    private fun handleClick(
        messageItem: TranscriptMessageItem,
        onItemListener: TranscriptAdapter.OnItemListener
    ) {
        if (messageItem.mediaStatus == MediaStatus.CANCELED.name) {
            if (messageItem.mediaUrl.isNullOrEmpty()) {
                onItemListener.onRetryDownload(messageItem.transcriptId, messageItem.messageId)
            } else {
                onItemListener.onRetryUpload(messageItem.transcriptId, messageItem.messageId)
            }
        } else if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
            onItemListener.onCancel(messageItem.transcriptId, messageItem.messageId)
        } else if (messageItem.mediaStatus != MediaStatus.EXPIRED.name) {
            onItemListener.onAudioClick(messageItem)
        }
    }
}
