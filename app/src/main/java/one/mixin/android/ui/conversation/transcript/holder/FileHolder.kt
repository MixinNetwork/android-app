package one.mixin.android.ui.conversation.transcript.holder

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import com.google.android.exoplayer2.util.MimeTypes
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatFileBinding
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.transcript.TranscriptAdapter
import one.mixin.android.util.MusicPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.TranscriptMessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip

class FileHolder constructor(val binding: ItemChatFileBinding) : BaseViewHolder(binding.root) {
    init {
        binding.billTime.chatFlag.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
    fun bind(
        messageItem: TranscriptMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: TranscriptAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        val isMe = messageItem.userId == Session.getAccountId()
        binding.billTime.chatSecret.isVisible = messageItem.isSignal()
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
        binding.fileNameTv.text = messageItem.mediaName
        when (messageItem.mediaStatus) {
            MediaStatus.EXPIRED.name -> {
                binding.bottomLayout.fileSizeTv.clearBindIdAndSetText(binding.root.context.getString(R.string.chat_expired))
            }
            MediaStatus.PENDING.name -> {
                messageItem.mediaSize?.notNullWithElse(
                    { it ->
                        binding.bottomLayout.fileSizeTv.setBindId(messageItem.messageId, it)
                    },
                    {
                        binding.bottomLayout.fileSizeTv.clearBindIdAndSetText(messageItem.mediaSize.fileSize())
                    }
                )
            }
            else -> {
                binding.bottomLayout.fileSizeTv.clearBindIdAndSetText(messageItem.mediaSize?.fileSize())
            }
        }
        setStatusIcon(isMe, MessageStatus.DELIVERED.name, isSecret = false, isRepresentative = false) { statusIcon, secretIcon, representativeIcon ->
            binding.billTime.chatFlag.isVisible = statusIcon != null
            binding.billTime.chatFlag.setImageDrawable(statusIcon)
            binding.billTime.chatSecret.isVisible = secretIcon != null
            binding.billTime.chatRepresentative.isVisible = representativeIcon != null
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
                    binding.bottomLayout.showText()
                    binding.bottomLayout.bindId = null
                    binding.chatLayout.setOnClickListener {
                    }
                }
                MediaStatus.PENDING.name -> {
                    binding.fileExpired.visibility = View.GONE
                    binding.fileProgress.visibility = View.VISIBLE
                    binding.fileProgress.enableLoading(MixinJobManager.getAttachmentProcess(messageItem.messageId))
                    binding.fileProgress.setBindOnly("${messageItem.transcriptId}${messageItem.messageId}")
                    binding.bottomLayout.showText()
                    binding.bottomLayout.bindId = null
                    binding.fileProgress.setOnClickListener {
                        onItemListener.onCancel(messageItem.transcriptId, messageItem.messageId)
                    }
                    binding.chatLayout.setOnClickListener {
                        handleClick(messageItem, onItemListener)
                    }
                }
                MediaStatus.DONE.name, MediaStatus.READ.name -> {
                    binding.fileExpired.visibility = View.GONE
                    binding.fileProgress.visibility = View.VISIBLE
                    if (MimeTypes.isAudio(messageItem.mediaMimeType)) {
                        binding.fileProgress.setBindOnly("${messageItem.transcriptId}${messageItem.messageId}")
                        binding.bottomLayout.bindId = "${messageItem.transcriptId}${messageItem.messageId}"
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
                            handleClick(messageItem, onItemListener)
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
                    binding.fileProgress.setBindId("${messageItem.transcriptId}${messageItem.messageId}")
                    binding.fileProgress.setProgress(-1)
                    binding.bottomLayout.showText()
                    binding.bottomLayout.bindId = null
                    binding.fileProgress.setOnClickListener {
                        handleClick(messageItem, onItemListener)
                    }
                    binding.chatLayout.setOnClickListener {
                        handleClick(messageItem, onItemListener)
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
                onItemListener.onRetryDownload(messageItem.transcriptId, messageItem.messageId)
            } else {
                onItemListener.onRetryUpload(messageItem.transcriptId, messageItem.messageId)
            }
        } else if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
            onItemListener.onCancel(messageItem.transcriptId, messageItem.messageId)
        } else if (messageItem.mediaStatus != MediaStatus.EXPIRED.name) {
            onItemListener.onFileClick(messageItem)
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
