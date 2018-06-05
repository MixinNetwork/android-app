package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
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
import org.jetbrains.anko.dip
import org.jetbrains.anko.textResource
import kotlin.math.min

class AudioHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    init {
        itemView.chat_flag.visibility = View.GONE
    }

    private val maxWidth by lazy {
        itemView.context.dpToPx(240f)
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
            itemView.setBackgroundColor(Color.parseColor("#660D94FC"))
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
            itemView.chat_name.setTextColor(colors[messageItem.userIdentityNumber.toLong().rem(colors.size).toInt()])
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
            (it.toLong()).let {
                itemView.chat_layout.layoutParams.width = min((minWidth + (it / 1000f) * dp15).toInt(), maxWidth)
            }
        }
        setStatusIcon(isMe, messageItem.status, {
            itemView.chat_flag.setImageDrawable(it)
            itemView.chat_flag.visibility = View.VISIBLE
        }, {
            itemView.chat_flag.visibility = View.GONE
        })
        messageItem.mediaWaveform?.let {
            itemView.audio_waveform.setWaveform(it)
        }
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.audio_expired.visibility = View.VISIBLE
                    itemView.audio_progress.visibility = View.INVISIBLE
                    itemView.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.PENDING.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    itemView.audio_progress.enableLoading()
                    itemView.audio_progress.setBindOnly(messageItem.messageId)
                    itemView.audio_progress.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                    itemView.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.DONE.name -> {
                    itemView.audio_expired.visibility = View.GONE
                    itemView.audio_progress.visibility = View.VISIBLE
                    itemView.audio_progress.setBindOnly(messageItem.messageId)
                    if (AudioPlayer.instance.isPlay(messageItem.messageId)) {
                        itemView.audio_progress.setPause()
                    } else {
                        itemView.audio_progress.setPlay()
                    }
                    itemView.audio_progress.setOnClickListener {
                        if (AudioPlayer.instance.isPlay(messageItem.messageId)) {
                            AudioPlayer.instance.stop()
                        } else {
                            AudioPlayer.instance.play(messageItem)
                        }
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }

                    itemView.setOnClickListener {
                        if (AudioPlayer.instance.isPlay(messageItem.messageId)) {
                            AudioPlayer.instance.stop()
                        } else {
                            AudioPlayer.instance.play(messageItem)
                        }
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
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
                    itemView.setOnClickListener {
                        handlerClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
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

    private fun handlerClick(
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
        } else if (messageItem.mediaStatus == MediaStatus.EXPIRED.name) {
        } else {
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
        if (isMe) {
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.bill_bubble_me_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.bill_bubble_me)
            }
            (itemView.chat_layout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.END
        } else {
            (itemView.chat_layout.layoutParams as LinearLayout.LayoutParams).gravity = Gravity.START
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            }
        }
    }
}