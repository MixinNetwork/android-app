package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.view.Gravity
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import com.google.gson.Gson
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_audio_quote.view.*
import kotlinx.android.synthetic.main.item_chat_audio_quote.view.chat_time
import one.mixin.android.R
import one.mixin.android.extension.*
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.AudioPlayer
import one.mixin.android.vo.*
import org.jetbrains.anko.textResource

class AudioQuoteHolder constructor(containerView: View) : MediaHolder(containerView) {
    private val dp15 = itemView.context.dpToPx(15f)
    private val dp16 = itemView.context.dpToPx(16f)
    private val dp8 = itemView.context.dpToPx(8f)
    private val maxWidth by lazy {
        itemView.context.dpToPx(255f)
    }

    private val minWidth by lazy {
        itemView.context.dpToPx(180f)
    }

    init {
        val radius = itemView.context.dpToPx(4f).toFloat()
        itemView.reply_layout.round(radius)
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
            isLast: Boolean,
            isFirst: Boolean = false,
            hasSelect: Boolean,
            isSelect: Boolean,
            onItemListener: ConversationAdapter.OnItemListener
    ) {
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
            itemView.audio_duration.text = messageItem.mediaDuration!!.toLong().formatMillis()
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
        val quoteMessage = Gson().fromJson(messageItem.quoteContent, QuoteMessageItem::class.java)
        itemView.reply_name_tv.text = quoteMessage.userFullName
        itemView.reply_name_tv.setTextColor(getColorById(quoteMessage.userId))
        itemView.reply_layout.setBackgroundColor(getColorById(quoteMessage.userId))
        itemView.reply_layout.background.alpha = 0x0D
        itemView.start_view.setBackgroundColor(getColorById(quoteMessage.userId))
        when {
            quoteMessage.type.endsWith("_TEXT") -> {
                itemView.reply_content_tv.text = quoteMessage.content
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp8
                setIcon()
            }
            quoteMessage.type == MessageCategory.MESSAGE_RECALL.name -> {
                itemView.reply_content_tv.setText(R.string.chat_recall_me)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp8
                setIcon(R.drawable.ic_status_recall)
            }
            quoteMessage.type.endsWith("_IMAGE") -> {
                itemView.reply_iv.loadImageCenterCrop(
                        quoteMessage.mediaUrl,
                        R.drawable.image_holder
                )
                itemView.reply_content_tv.setText(R.string.photo)
                setIcon(R.drawable.ic_status_pic)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp16
            }
            quoteMessage.type.endsWith("_VIDEO") -> {
                itemView.reply_iv.loadImageCenterCrop(
                        quoteMessage.mediaUrl,
                        R.drawable.image_holder
                )
                itemView.reply_content_tv.setText(R.string.video)
                setIcon(R.drawable.ic_status_video)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp16
            }
            quoteMessage.type.endsWith("_LIVE") -> {
                itemView.reply_iv.loadImageCenterCrop(
                        quoteMessage.thumbUrl,
                        R.drawable.image_holder
                )
                itemView.reply_content_tv.setText(R.string.live)
                setIcon(R.drawable.ic_status_live)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp16
            }
            quoteMessage.type.endsWith("_DATA") -> {
                quoteMessage.mediaName.notNullWithElse({
                    itemView.reply_content_tv.text = it
                }, {
                    itemView.reply_content_tv.setText(R.string.document)
                })
                setIcon(R.drawable.ic_status_file)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp8
            }
            quoteMessage.type.endsWith("_AUDIO") -> {
                quoteMessage.mediaDuration.notNullWithElse({
                    itemView.reply_content_tv.text = it.toLong().formatMillis()
                }, {
                    itemView.reply_content_tv.setText(R.string.audio)
                })
                setIcon(R.drawable.ic_status_audio)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp8
            }
            quoteMessage.type.endsWith("_STICKER") -> {
                itemView.reply_content_tv.setText(R.string.conversation_status_sticker)
                setIcon(R.drawable.ic_status_stiker)
                itemView.reply_iv.loadImageCenterCrop(
                        quoteMessage.assetUrl,
                        R.drawable.image_holder
                )
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp16
            }
            quoteMessage.type.endsWith("_CONTACT") -> {
                itemView.reply_content_tv.text = quoteMessage.sharedUserIdentityNumber
                setIcon(R.drawable.ic_status_contact)
                itemView.reply_avatar.setInfo(
                        quoteMessage.sharedUserFullName,
                        quoteMessage.sharedUserAvatarUrl,
                        quoteMessage.sharedUserId
                                ?: "0"
                )
                itemView.reply_avatar.visibility = View.VISIBLE
                itemView.reply_iv.visibility = View.INVISIBLE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp16
            }
            quoteMessage.type == MessageCategory.APP_BUTTON_GROUP.name || quoteMessage.type == MessageCategory.APP_CARD.name -> {
                itemView.reply_content_tv.setText(R.string.extensions)
                setIcon(R.drawable.ic_touch_app)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd =
                        dp8
            }
            else -> {
                itemView.reply_iv.visibility = View.GONE
            }
        }
        chatLayout(isMe, isLast)
    }

    private fun setIcon(@DrawableRes icon: Int? = null) {
        icon.notNullWithElse({ drawable ->
            AppCompatResources.getDrawable(itemView.context, drawable).let {
                it?.setBounds(0, 0, itemView.context.dpToPx(10f), itemView.context.dpToPx(10f))
                TextViewCompat.setCompoundDrawablesRelative(
                        itemView.reply_content_tv,
                        it,
                        null,
                        null,
                        null
                )
            }
        }, {
            TextViewCompat.setCompoundDrawablesRelative(
                    itemView.reply_content_tv,
                    null,
                    null,
                    null,
                    null
            )
        })
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
}
