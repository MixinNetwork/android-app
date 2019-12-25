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
import kotlinx.android.synthetic.main.item_chat_contact_card_quote.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadImageCenterCrop
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.util.Session
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.vo.mediaDownloaded
import one.mixin.android.vo.showVerifiedOrBot
import org.jetbrains.anko.dip

class ContactCardQuoteHolder constructor(containerView: View) : MediaHolder(containerView) {
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
        itemView.chat_time.round(radius)
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
        item: MessageItem,
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
        itemView.avatar_iv.setInfo(item.sharedUserFullName, item.sharedUserAvatarUrl, item.sharedUserId
            ?: "0")
        itemView.name_tv.text = item.sharedUserFullName
        itemView.id_tv.text = item.sharedUserIdentityNumber
        itemView.chat_time.timeAgoClock(item.createdAt)
        item.showVerifiedOrBot(itemView.verified_iv, itemView.bot_iv)

        val isMe = Session.getAccountId() == item.userId
        if (isFirst && !isMe) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = item.userFullName
            if (item.appId != null) {
                itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
                itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
            } else {
                itemView.chat_name.setCompoundDrawables(null, null, null, null)
            }
            itemView.chat_name.setTextColor(getColorById(item.userId))
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(item.userId) }
        } else {
            itemView.chat_name.visibility = View.GONE
        }

        setStatusIcon(isMe, item.status, {
            itemView.chat_flag.setImageDrawable(it)
            itemView.chat_flag.visibility = View.VISIBLE
        }, {
            itemView.chat_flag.visibility = View.GONE
        })
        chatLayout(isMe, isLast)

        itemView.chat_layout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onContactCardClick(item.sharedUserId!!)
            } else {
                onItemListener.onSelect(!isSelect, item, adapterPosition)
            }
        }
        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, item, adapterPosition)
            }
        }
        itemView.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(item, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, item, adapterPosition)
                true
            }
        }
        itemView.chat_layout.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(item, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, item, adapterPosition)
                true
            }
        }
        val quoteMessage = Gson().fromJson(item.quoteContent, QuoteMessageItem::class.java)
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
            quoteMessage.type.endsWith("_POST") -> {
                itemView.reply_content_tv.setText(R.string.post)
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
