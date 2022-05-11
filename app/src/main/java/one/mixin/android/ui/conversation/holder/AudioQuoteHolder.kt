package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.Constants.Colors.SELECT_COLOR
import one.mixin.android.R
import one.mixin.android.databinding.ItemChatAudioQuoteBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.round
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.base.MediaHolder
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.isSecret
import one.mixin.android.vo.mediaDownloaded

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
            (binding.chatMsgLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 1f
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
            (binding.chatMsgLayout.layoutParams as ConstraintLayout.LayoutParams).horizontalBias = 0f
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

    private var onItemListener: ConversationAdapter.OnItemListener? = null

    fun bind(
        messageItem: MessageItem,
        isFirst: Boolean,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        isRepresentative: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        super.bind(messageItem)
        val isMe = meId == messageItem.userId
        this.onItemListener = onItemListener
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
            binding.audioDuration.setText(R.string.Expired)
        } else {
            binding.audioDuration.text = messageItem.mediaDuration?.toLongOrNull()?.formatMillis() ?: ""
        }
        binding.chatTime.load(
            isMe,
            messageItem.createdAt,
            messageItem.status,
            messageItem.isPin ?: false,
            isRepresentative = isRepresentative,
            isSecret = messageItem.isSecret(),
        )

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
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.PENDING.name -> {
                    binding.audioExpired.visibility = View.GONE
                    binding.audioProgress.visibility = View.VISIBLE
                    binding.audioProgress.enableLoading(getAttachmentProcess(messageItem.messageId))
                    binding.audioProgress.setBindOnly(messageItem.messageId)
                    binding.audioProgress.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                    binding.chatLayout.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
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
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                    binding.chatLayout.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
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
        val quoteMessage =
            GsonHelper.customGson.fromJson(messageItem.quoteContent, QuoteMessageItem::class.java)
        binding.chatQuote.bind(quoteMessage)
        binding.chatQuote.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onQuoteMessageClick(messageItem.messageId, messageItem.quoteId)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, absoluteAdapterPosition)
            }
        }
        chatLayout(isMe, isLast)
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
        } else if (mediaDownloaded(messageItem.mediaStatus)) {
            onItemListener.onAudioClick(messageItem)
        } else if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
        } else {
        }
    }
}
