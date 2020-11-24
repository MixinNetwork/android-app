package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import androidx.core.widget.TextViewCompat
import kotlinx.android.synthetic.main.item_chat_audio_quote.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.GsonHelper
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.mediaDownloaded
import org.jetbrains.anko.dip
import org.jetbrains.anko.textResource

class AudioQuoteHolder constructor(containerView: View) : MediaHolder(containerView) {
    private val maxWidth by lazy {
        itemView.context.dpToPx(255f)
    }

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        itemView.chat_audio_layout.round(radius)
        itemView.chat_time.round(radius)
        itemView.chat_audio_layout.layoutParams.width = maxWidth
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            itemView.chat_msg_layout.gravity = Gravity.END
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_me_last,
                    R.drawable.chat_bubble_reply_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_me,
                    R.drawable.chat_bubble_reply_me_night
                )
            }
        } else {
            itemView.chat_msg_layout.gravity = Gravity.START
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_reply_other_last,
                    R.drawable.chat_bubble_reply_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
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
        itemView.chat_time.timeAgoClock(messageItem.createdAt)

        if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
            itemView.audio_duration.textResource = R.string.chat_expired
        } else {
            itemView.audio_duration.text = messageItem.mediaDuration?.toLongOrNull()?.formatMillis() ?: ""
        }
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal(), isRepresentative) { statusIcon, secretIcon, representativeIcon ->
            statusIcon?.setBounds(0, 0, dp12, dp12)
            secretIcon?.setBounds(0, 0, dp8, dp8)
            representativeIcon?.setBounds(0, 0, dp8, dp8)
            TextViewCompat.setCompoundDrawablesRelative(itemView.chat_time, secretIcon ?: representativeIcon, null, statusIcon, null)
        }

        if (isFirst && !isMe) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = messageItem.userFullName
            if (messageItem.appId != null) {
                itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
                itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
            } else {
                itemView.chat_name.setCompoundDrawables(null, null, null, null)
            }
            itemView.chat_name.setTextColor(getColorById(messageItem.userId))
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }

        messageItem.mediaWaveform?.let {
            itemView.audio_waveform.setWaveform(it)
        }
        if (!isMe && messageItem.mediaStatus != MediaStatus.READ.name) {
            itemView.audio_duration.setTextColor(itemView.context.getColor(R.color.colorBlue))
            itemView.audio_waveform.isFresh = true
        } else {
            itemView.audio_duration.setTextColor(itemView.context.getColor(R.color.gray_50))
            itemView.audio_waveform.isFresh = false
        }
        if (AudioPlayer.isLoaded(messageItem.messageId)) {
            itemView.audio_waveform.setProgress(AudioPlayer.getProgress())
        } else {
            itemView.audio_waveform.setProgress(0f)
        }
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.audio_expired.visibility = View.VISIBLE
                    itemView.audio_progress.visibility = View.INVISIBLE
                    itemView.chat_layout.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.PENDING.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    itemView.audio_progress.enableLoading(getAttachmentProcess(messageItem.messageId))
                    itemView.audio_progress.setBindOnly(messageItem.messageId)
                    itemView.audio_progress.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                    itemView.chat_layout.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.DONE.name, MediaStatus.READ.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    itemView.audio_progress.setBindOnly(messageItem.messageId)
                    itemView.audio_waveform.setBind(messageItem.messageId)
                    if (AudioPlayer.isPlay(messageItem.messageId)) {
                        itemView.audio_progress.setPause()
                    } else {
                        itemView.audio_progress.setPlay()
                    }
                    itemView.audio_progress.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                    itemView.chat_layout.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.CANCELED.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    if (isMe) {
                        itemView.audio_progress.enableUpload()
                    } else {
                        itemView.audio_progress.enableDownload()
                    }
                    itemView.audio_progress.setBindOnly(messageItem.messageId)
                    itemView.audio_progress.setProgress(-1)
                    itemView.audio_progress.setOnClickListener {
                        if (isMe) {
                            onItemListener.onRetryUpload(messageItem.messageId)
                        } else {
                            onItemListener.onRetryDownload(messageItem.messageId)
                        }
                    }
                    itemView.chat_layout.setOnClickListener {
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
        itemView.chat_layout.setOnLongClickListener {
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
        itemView.chat_quote.bind(quoteMessage)
        itemView.chat_quote.setOnClickListener {
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
