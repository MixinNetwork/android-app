package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import kotlin.math.min
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_audio.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.AudioPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal
import one.mixin.android.vo.mediaDownloaded
import org.jetbrains.anko.dip
import org.jetbrains.anko.textResource

class AudioHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    init {
        itemView.chat_flag.visibility = View.GONE
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
        messageItem: MessageItem,
        isFirst: Boolean,
        isLast: Boolean,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(SELECT_COLOR)
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        val isMe = meId == messageItem.userId
        chatLayout(isMe, isLast)
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
        itemView.chat_time.timeAgoClock(messageItem.createdAt)

        if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
            itemView.audio_duration.textResource = R.string.chat_expired
        } else {
            itemView.audio_duration.text = messageItem.mediaDuration!!.toLong().formatMillis()
        }

        messageItem.mediaDuration?.let {
            val duration = try {
                it.toLong()
            } catch (e: Exception) {
                0L
            }
            itemView.chat_layout.layoutParams.width =
                min((minWidth + (duration / 1000f) * dp15).toInt(), maxWidth)
        }
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal()) { statusIcon, secretIcon ->
            itemView.chat_flag.isVisible = statusIcon != null
            itemView.chat_flag.setImageDrawable(statusIcon)
            itemView.chat_secret.isVisible = secretIcon != null
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
        if (AudioPlayer.get().isLoaded(messageItem.messageId)) {
            itemView.audio_waveform.setProgress(AudioPlayer.get().progress)
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
                    itemView.audio_progress.enableLoading()
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
                    if (AudioPlayer.get().isPlay(messageItem.messageId)) {
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
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }
        itemView.chat_layout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
            }
        }
        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
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
            onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
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

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isMe) {
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.bill_bubble_me_last,
                    R.drawable.bill_bubble_me_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.bill_bubble_me,
                    R.drawable.bill_bubble_me_night
                )
            }
            (itemView.chat_layout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
        } else {
            (itemView.chat_layout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START
            if (isLast) {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_other_last,
                    R.drawable.chat_bubble_other_last_night
                )
            } else {
                setItemBackgroundResource(
                    itemView.chat_layout,
                    R.drawable.chat_bubble_other,
                    R.drawable.chat_bubble_other_night
                )
            }
        }
    }
}
