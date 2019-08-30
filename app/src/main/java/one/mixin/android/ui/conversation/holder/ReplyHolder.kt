package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import com.google.gson.Gson
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_reply.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadImageCenterCrop
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import one.mixin.android.widget.linktext.AutoLinkMode
import org.jetbrains.anko.dip

class ReplyHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    private val dp16 = itemView.context.dpToPx(16f)
    private val dp8 = itemView.context.dpToPx(8f)
    private val dp6 = itemView.context.dpToPx(6f)

    init {
        itemView.chat_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        itemView.chat_tv.setUrlModeColor(LINK_COLOR)
        itemView.chat_name.maxWidth = itemView.context.maxItemWidth() - dp16
        itemView.chat_msg_content.setMaxWidth(itemView.context.maxItemWidth() - dp16)
        itemView.chat_tv.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener?.onUrlClick(matchedText)
                }
                AutoLinkMode.MODE_MENTION -> {
                    onItemListener?.onMentionClick(matchedText)
                }
                else -> {
                }
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        val lp = (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams)
        if (isMe) {
            lp.gravity = Gravity.END
            if (isLast) {
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_reply_me_last)
            } else {
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_reply_me)
            }
        } else {
            lp.gravity = Gravity.START
            if (isLast) {
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_reply_other_last)
            } else {
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_reply_other)
            }
        }
    }

    init {
        itemView.reply_layout.round(dp6)
        itemView.reply_iv.round(dp6)
    }

    private var onItemListener: ConversationAdapter.OnItemListener? = null

    fun bind(
        messageItem: MessageItem,
        keyword: String?,
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

        itemView.chat_tv.setOnLongClickListener {
            if (!hasSelect) {
                onItemListener.onLongClick(messageItem, adapterPosition)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
                true
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

        itemView.chat_tv.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
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

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        keyword.notNullWithElse({ k ->
            messageItem.content?.let { str ->
                val start = str.indexOf(k, 0, true)
                if (start >= 0) {
                    val sp = SpannableString(str)
                    sp.setSpan(BackgroundColorSpan(HIGHLIGHTED), start,
                        start + k.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    itemView.chat_tv.text = sp
                } else {
                    itemView.chat_tv.text = messageItem.content
                }
            }
        }, {
            itemView.chat_tv.text = messageItem.content
        })

        val isMe = meId == messageItem.userId
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

        if (messageItem.appId != null) {
            itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
            itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
        } else {
            itemView.chat_name.setCompoundDrawables(null, null, null, null)
        }
        setStatusIcon(isMe, messageItem.status, {
            itemView.chat_flag.setImageDrawable(it)
            itemView.chat_flag.visibility = View.VISIBLE
        }, {
            itemView.chat_flag.visibility = View.GONE
        })

        itemView.chat_layout.setOnClickListener {
            if (!hasSelect) {
                onItemListener.onMessageClick(messageItem.quoteId)
            } else {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
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
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                setIcon()
            }
            quoteMessage.type == MessageCategory.MESSAGE_RECALL.name -> {
                itemView.reply_content_tv.setText(R.string.chat_recall_me)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                setIcon(R.drawable.ic_status_recall)
            }
            quoteMessage.type.endsWith("_IMAGE") -> {
                itemView.reply_iv.loadImageCenterCrop(quoteMessage.mediaUrl, R.drawable.image_holder)
                itemView.reply_content_tv.setText(R.string.photo)
                setIcon(R.drawable.ic_status_pic)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
            }
            quoteMessage.type.endsWith("_VIDEO") -> {
                itemView.reply_iv.loadImageCenterCrop(quoteMessage.mediaUrl, R.drawable.image_holder)
                itemView.reply_content_tv.setText(R.string.video)
                setIcon(R.drawable.ic_status_video)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
            }
            quoteMessage.type.endsWith("_LIVE") -> {
                itemView.reply_iv.loadImageCenterCrop(quoteMessage.thumbUrl, R.drawable.image_holder)
                itemView.reply_content_tv.setText(R.string.live)
                setIcon(R.drawable.ic_status_live)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
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
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
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
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
            }
            quoteMessage.type.endsWith("_STICKER") -> {
                itemView.reply_content_tv.setText(R.string.conversation_status_sticker)
                setIcon(R.drawable.ic_status_stiker)
                itemView.reply_iv.loadImageCenterCrop(quoteMessage.assetUrl, R.drawable.image_holder)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
            }
            quoteMessage.type.endsWith("_CONTACT") -> {
                itemView.reply_content_tv.text = quoteMessage.sharedUserIdentityNumber
                setIcon(R.drawable.ic_status_contact)
                itemView.reply_avatar.setInfo(quoteMessage.sharedUserFullName, quoteMessage.sharedUserAvatarUrl, quoteMessage.sharedUserId
                    ?: "0")
                itemView.reply_avatar.visibility = View.VISIBLE
                itemView.reply_iv.visibility = View.INVISIBLE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp16
            }
            quoteMessage.type == MessageCategory.APP_BUTTON_GROUP.name || quoteMessage.type == MessageCategory.APP_CARD.name -> {
                itemView.reply_content_tv.setText(R.string.extensions)
                setIcon(R.drawable.ic_touch_app)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                (itemView.reply_content_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
                (itemView.reply_name_tv.layoutParams as ConstraintLayout.LayoutParams).marginEnd = dp8
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
                TextViewCompat.setCompoundDrawablesRelative(itemView.reply_content_tv, it, null, null, null)
            }
        }, {
            TextViewCompat.setCompoundDrawablesRelative(itemView.reply_content_tv, null, null, null, null)
        })
    }
}
