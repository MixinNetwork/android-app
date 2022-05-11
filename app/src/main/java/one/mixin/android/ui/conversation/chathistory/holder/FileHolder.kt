package one.mixin.android.ui.conversation.chathistory.holder

import android.annotation.SuppressLint
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.exoplayer2.util.MimeTypes
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatFileBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.ui.conversation.chathistory.ChatHistoryAdapter
import one.mixin.android.util.MusicPlayer
import one.mixin.android.vo.ChatHistoryMessageItem
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageStatus

class FileHolder constructor(val binding: ItemChatFileBinding) : BaseViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(
        messageItem: ChatHistoryMessageItem,
        isLast: Boolean,
        isFirst: Boolean = false,
        onItemListener: ChatHistoryAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        val isMe = messageItem.userId == Session.getAccountId()
        chatLayout(isMe, isLast)
        if (isFirst && !isMe) {
            binding.chatName.visibility = View.VISIBLE
            binding.chatName.text = messageItem.userFullName
            if (messageItem.appId != null) {
                binding.chatName.setCompoundDrawables(null, null, botIcon, null)
                binding.chatName.compoundDrawablePadding = 3.dp
            } else {
                binding.chatName.setCompoundDrawables(null, null, null, null)
            }
            binding.chatName.setTextColor(getColorById(messageItem.userId))
            binding.chatName.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            binding.chatName.visibility = View.GONE
        }
        binding.fileNameTv.text = messageItem.mediaName
        when (messageItem.mediaStatus) {
            MediaStatus.EXPIRED.name -> {
                binding.bottomLayout.fileSizeTv.clearBindIdAndSetText(binding.root.context.getString(R.string.Expired))
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
        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            MessageStatus.DELIVERED.name,
            false,
            isRepresentative = false,
            isSecret = false
        )
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
                    binding.fileProgress.setBindOnly("${messageItem.transcriptId ?: ""}${messageItem.messageId}")
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
                    binding.fileProgress.setDone()
                    binding.fileProgress.setBindId(null)
                    binding.bottomLayout.bindId = null
                    binding.fileProgress.setOnClickListener {
                        handleClick(messageItem, onItemListener)
                    }
                    binding.chatLayout.setOnClickListener {

                        handleClick(messageItem, onItemListener)
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
                    binding.fileProgress.setBindId("${messageItem.transcriptId ?: ""}${messageItem.messageId}")
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
        if (messageItem.transcriptId == null) {
            binding.root.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            binding.chatLayout.setOnLongClickListener {
                onItemListener.onMenu(binding.chatJump, messageItem)
                true
            }
            chatJumpLayout(binding.chatJump, isMe, messageItem.messageId, R.id.chat_msg_layout, onItemListener)
        }
    }

    private fun handleClick(
        messageItem: ChatHistoryMessageItem,
        onItemListener: ChatHistoryAdapter.OnItemListener
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
            (binding.chatMsgLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
        } else {
            (binding.chatMsgLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
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
