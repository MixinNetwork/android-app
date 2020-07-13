package one.mixin.android.ui.conversation.holder

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import com.google.android.exoplayer2.util.MimeTypes
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_file.view.*
import kotlinx.android.synthetic.main.layout_file_holder_bottom.view.*
import one.mixin.android.R
import one.mixin.android.extension.fileSize
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.job.MixinJobManager.Companion.getAttachmentProcess
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.AudioPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isSignal
import org.jetbrains.anko.dip
import org.jetbrains.anko.textResource

class FileHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    init {
        itemView.chat_flag.visibility = View.GONE
    }

    @SuppressLint("SetTextI18n")
    fun bind(
        messageItem: MessageItem,
        keyword: String?,
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
        itemView.chat_secret.isVisible = messageItem.isSignal()
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
        keyword.notNullWithElse(
            { k ->
                messageItem.mediaName?.let { str ->
                    val start = str.indexOf(k, 0, true)
                    if (start >= 0) {
                        val sp = SpannableString(str)
                        sp.setSpan(
                            BackgroundColorSpan(HIGHLIGHTED), start,
                            start + k.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        itemView.file_name_tv.text = sp
                    } else {
                        itemView.file_name_tv.text = messageItem.mediaName
                    }
                }
            },
            {
                itemView.file_name_tv.text = messageItem.mediaName
            }
        )
        when (messageItem.mediaStatus) {
            MediaStatus.EXPIRED.name -> {
                itemView.file_size_tv.textResource = R.string.chat_expired
            }
            MediaStatus.PENDING.name -> {
                messageItem.mediaSize?.notNullWithElse(
                    { it ->
                        itemView.file_size_tv.setBindId(messageItem.messageId, it)
                    },
                    {
                        itemView.file_size_tv.text = messageItem.mediaSize.fileSize()
                    }
                )
            }
            else -> {
                itemView.file_size_tv.text = "${messageItem.mediaSize?.fileSize()}"
            }
        }
        setStatusIcon(isMe, messageItem.status, messageItem.isSignal()) { statusIcon, secretIcon ->
            itemView.chat_flag.isVisible = statusIcon != null
            itemView.chat_flag.setImageDrawable(statusIcon)
            itemView.chat_secret.isVisible = secretIcon != null
        }
        itemView.seek_bar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (MimeTypes.isAudio(messageItem.mediaMimeType) &&
                    AudioPlayer.get().isPlay(messageItem.messageId)
                ) {
                    AudioPlayer.get().seekTo(seekBar.progress)
                }
            }
        })
        messageItem.mediaStatus?.let {
            when (it) {
                MediaStatus.EXPIRED.name -> {
                    itemView.file_expired.visibility = View.VISIBLE
                    itemView.file_progress.visibility = View.INVISIBLE
                    itemView.chat_layout.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.PENDING.name -> {
                    itemView.file_expired.visibility = View.GONE
                    itemView.file_progress.visibility = View.VISIBLE
                    itemView.file_progress.enableLoading(getAttachmentProcess(messageItem.messageId))
                    itemView.file_progress.setBindOnly(messageItem.messageId)
                    itemView.file_progress.setOnClickListener {
                        onItemListener.onCancel(messageItem.messageId)
                    }
                    itemView.chat_layout.setOnClickListener {
                        handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                    }
                }
                MediaStatus.DONE.name, MediaStatus.READ.name -> {
                    itemView.file_expired.visibility = View.GONE
                    itemView.file_progress.visibility = View.VISIBLE
                    if (MimeTypes.isAudio(messageItem.mediaMimeType)) {
                        itemView.file_progress.setBindOnly(messageItem.messageId)
                        itemView.bottom_layout.bindId = messageItem.messageId
                        if (AudioPlayer.get().isPlay(messageItem.messageId)) {
                            itemView.file_progress.setPause()
                            itemView.bottom_layout.showSeekBar()
                        } else {
                            itemView.file_progress.setPlay()
                            itemView.bottom_layout.showText()
                        }
                        itemView.file_progress.setOnClickListener {
                            onItemListener.onAudioFileClick(messageItem)
                        }
                    } else {
                        itemView.file_progress.setDone()
                        itemView.file_progress.setBindId(null)
                        itemView.bottom_layout.bindId = null
                        itemView.file_progress.setOnClickListener {
                            handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                        }
                    }
                    itemView.chat_layout.setOnClickListener {
                        if (AudioPlayer.get().isPlay(messageItem.messageId)) {
                            onItemListener.onAudioFileClick(messageItem)
                        } else {
                            handleClick(hasSelect, isSelect, isMe, messageItem, onItemListener)
                        }
                    }
                }
                MediaStatus.CANCELED.name -> {
                    itemView.file_expired.visibility = View.GONE
                    itemView.file_progress.visibility = View.VISIBLE
                    if (isMe && messageItem.mediaUrl != null) {
                        itemView.file_progress.enableUpload()
                    } else {
                        itemView.file_progress.enableDownload()
                    }
                    itemView.file_progress.setBindId(messageItem.messageId)
                    itemView.file_progress.setProgress(-1)
                    itemView.file_progress.setOnClickListener {
                        if (isMe && messageItem.mediaUrl != null) {
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
