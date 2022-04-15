package one.mixin.android.ui.conversation.holder

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.View
import android.widget.SeekBar
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.exoplayer2.util.MimeTypes
import one.mixin.android.Constants.Colors.HIGHLIGHTED
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatFileBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.BaseViewHolder
import one.mixin.android.util.MusicPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSecret

class FileHolder constructor(val binding: ItemChatFileBinding) : BaseViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(
        messageItem: MessageItem,
        keyword: String?,
        isFirst: Boolean,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }
        val isMe = meId == messageItem.userId
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
        keyword.notNullWithElse(
            { k ->
                messageItem.mediaName?.let { str ->
                    val start = str.indexOf(k, 0, true)
                    if (start >= 0) {
                        val sp = SpannableString(str)
                        sp.setSpan(
                            BackgroundColorSpan(HIGHLIGHTED),
                            start,
                            start + k.length,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.fileNameTv.text = sp
                    } else {
                        binding.fileNameTv.text = messageItem.mediaName
                    }
                }
            },
            {
                binding.fileNameTv.text = messageItem.mediaName
            }
        )
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
            messageItem.status,
            false,
            isRepresentative = isRepresentative,
            isSecret = messageItem.isSecret(),
            isWhite = false
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
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.PENDING.name -> {
                    binding.fileExpired.visibility = View.GONE
                    binding.fileProgress.visibility = View.VISIBLE
                    binding.fileProgress.enableLoading(getAttachmentProcess(messageItem.messageId))
                    binding.fileProgress.setBindOnly(messageItem.messageId)
                    binding.bottomLayout.showText()
                    binding.bottomLayout.bindId = null
                    binding.fileProgress.setOnClickListener {
                        onItemListener.onCancel(messageItem.messageId)
                    }
                    binding.chatLayout.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
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
                            handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                        }
                    }
                    binding.chatLayout.setOnClickListener {
                        if (MusicPlayer.isPlay(messageItem.messageId)) {
                            onItemListener.onAudioFileClick(messageItem)
                        } else {
                            handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
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
                    binding.bottomLayout.showText()
                    binding.bottomLayout.bindId = null
                    binding.fileProgress.setOnClickListener {
                        if (isMe && messageItem.mediaUrl != null) {
                            onItemListener.onRetryUpload(messageItem.messageId)
                        } else {
                            onItemListener.onRetryDownload(messageItem.messageId)
                        }
                    }
                    binding.chatLayout.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
            }
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }
        binding.chatLayout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, absoluteAdapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
                true
            }
        }
    }

    private fun handleClick(
        hasSelect: Boolean,
        isSelect: Boolean,
        isMe: Boolean,
        messageItem: MessageItem,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        if (hasSelect) {
            onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
        } else if (messageItem.mediaStatus == MediaStatus.CANCELED.name) {
            if (isMe) {
                onItemListener.onRetryUpload(messageItem.messageId)
            } else {
                onItemListener.onRetryDownload(messageItem.messageId)
            }
        } else if (messageItem.mediaStatus == MediaStatus.PENDING.name) {
            onItemListener.onCancel(messageItem.messageId)
        } else if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
        } else {
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
