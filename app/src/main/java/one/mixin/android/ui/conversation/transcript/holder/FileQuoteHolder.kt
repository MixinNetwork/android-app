package one.mixin.android.ui.conversation.transcript.holder

import android.view.Gravity
import android.view.View
import android.widget.SeekBar
import androidx.core.widget.TextViewCompat
import com.google.android.exoplayer2.util.MimeTypes
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatFileQuoteBinding
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.job.MixinJobManager
import one.mixin.android.ui.conversation.transcript.TranscriptAdapter
import one.mixin.android.util.GsonHelper
import one.mixin.android.util.MusicPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.TranscriptMessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip
import org.jetbrains.anko.textResource

class FileQuoteHolder constructor(val binding: ItemChatFileQuoteBinding) : MediaHolder(binding.root) {
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
        binding.chatTime.timeAgoClock(messageItem.createdAt)
        setStatusIcon(isMe, MessageStatus.DELIVERED.name, messageItem.isSignal(), false) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(binding.chatTime, secretIcon ?: representativeIcon, null, statusIcon, null)
        }

        binding.fileNameTv.text = messageItem.mediaName
        if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
            binding.bottomLayout.fileSizeTv.textResource = R.string.chat_expired
        } else {
            binding.bottomLayout.fileSizeTv.text = "${messageItem.mediaSize?.fileSize()}"
        }

        binding.bottomLayout.seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    if (MimeTypes.isAudio(messageItem.mediaMimeType) &&
                        MusicPlayer.isPlay(messageItem.messageId)
                    ) {
                        MusicPlayer.seekTo(seekBar.progress)
                    }
                }
            }
        )
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    binding.fileExpired.visibility = View.VISIBLE
                    binding.fileProgress.visibility = View.INVISIBLE
                    binding.chatLayout.setOnClickListener {
                    }
                }
                MediaStatus.PENDING.name -> {
                    binding.fileExpired.visibility = View.GONE
                    binding.fileProgress.visibility = View.VISIBLE
                    binding.fileProgress.enableLoading(MixinJobManager.getAttachmentProcess(messageItem.messageId))
                    binding.fileProgress.setBindOnly(messageItem.messageId)
                    binding.fileProgress.setOnClickListener {
                        onItemListener.onCancel(messageItem.messageId)
                    }
                    binding.chatLayout.setOnClickListener {
                    }
                }
                MediaStatus.DONE.name, MediaStatus.READ.name -> {
                    binding.fileExpired.visibility = View.GONE
                    binding.fileProgress.visibility = View.VISIBLE
                    if (MimeTypes.isAudio(messageItem.mediaMimeType)) {
                        binding.fileProgress.setBindOnly(messageItem.messageId)
                        binding.bottomLayout.bindId = messageItem.messageId
                        if (MusicPlayer.isPlay(messageItem.messageId)) {
                            binding.fileProgress.setPause()
                            binding.bottomLayout.showSeekBar()
                        } else {
                            binding.fileProgress.setPlay()
                            binding.bottomLayout.showText()
                        }
                        binding.fileProgress.setOnClickListener {
                            onItemListener.onAudioFileClick(messageItem)
                        }
                    } else {
                        binding.fileProgress.setDone()
                        binding.fileProgress.setBindId(null)
                        binding.bottomLayout.bindId = null
                        binding.fileProgress.setOnClickListener {
                        }
                    }
                    binding.chatLayout.setOnClickListener {
                        if (MusicPlayer.isPlay(messageItem.messageId)) {
                            onItemListener.onAudioFileClick(messageItem)
                        } else {
                            handleClick(messageItem, onItemListener)
                        }
                    }
                }
                MediaStatus.CANCELED.name -> {
                    binding.fileExpired.visibility = View.GONE
                    binding.fileProgress.visibility = View.VISIBLE
                    if (isMe && messageItem.mediaUrl != null) {
                        binding.fileProgress.enableUpload()
                    } else {
                        binding.fileProgress.enableDownload()
                    }
                    binding.fileProgress.setBindId(messageItem.messageId)
                    binding.fileProgress.setProgress(-1)
                    binding.fileProgress.setOnClickListener {
                        if (isMe && messageItem.mediaUrl != null) {
                            onItemListener.onRetryUpload(messageItem.messageId)
                        } else {
                            onItemListener.onRetryDownload(messageItem.messageId)
                        }
                    }
                    binding.chatLayout.setOnClickListener {
                        handleClick(messageItem, onItemListener)
                    }
                }
            }
        }
        val quoteMessage = GsonHelper.customGson.fromJson(messageItem.quoteContent, QuoteMessageItem::class.java)
        binding.chatQuote.bind(quoteMessage)
        binding.chatQuote.setOnClickListener {
            onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
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
            onItemListener.onFileClick(messageItem)
        }
    }
}
