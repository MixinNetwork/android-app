package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.support.annotation.DrawableRes
import android.support.v4.widget.TextViewCompat
import android.support.v7.content.res.AppCompatResources
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import com.google.gson.Gson
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_reply.view.*
import one.mixin.android.R
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.formatMillis
import one.mixin.android.extension.loadImageCenterCrop
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.round
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.QuoteMessageItem
import org.jetbrains.anko.dip

class ReplyHolder constructor(containerView: View) : BaseViewHolder(containerView) {
    private val dp8 = itemView.context.dpToPx(8f)
    private val dp6 = itemView.context.dpToPx(6f)
    private val dp1 = itemView.context.dpToPx(1f)

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
        val lp = (itemView.chat_layout.layoutParams as FrameLayout.LayoutParams)
        if (isMe) {
            lp.gravity = Gravity.END
            if (isLast) {
                lp.bottomMargin = dp8
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_reply_me_last)
            } else {
                lp.bottomMargin = dp1
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_reply_me)
            }
        } else {
            lp.gravity = Gravity.START
            if (isLast) {
                lp.bottomMargin = dp8
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_reply_other_last)
            } else {
                lp.bottomMargin = dp1
                itemView.chat_msg_layout.setBackgroundResource(R.drawable.chat_bubble_reply_other)
            }
        }
    }

    init {
        itemView.reply_layout.round(dp6)
        itemView.reply_iv.round(dp6)
    }

    private var onItemListener: ConversationAdapter.OnItemListener? = null

    fun bind(messageItem: MessageItem,
        keyword: String?,
        isLast: Boolean,
        isFirst: Boolean = false,
        hasSelect: Boolean,
        isSelect: Boolean,
        onItemListener: ConversationAdapter.OnItemListener) {
        listen(messageItem.messageId)
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
        notNullElse(keyword, { k ->
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
            itemView.chat_name.setTextColor(colors[messageItem.userIdentityNumber.toLong().rem(colors.size).toInt()])
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
        itemView.reply_name_tv.setTextColor(colors[quoteMessage.userIdentityNumber.toLong().rem(colors.size).toInt()])
        itemView.reply_layout.setBackgroundColor(colors[quoteMessage.userIdentityNumber.toLong().rem(colors.size).toInt()])
        itemView.reply_layout.background.alpha = 0x11
        itemView.start_view.setBackgroundColor(colors[quoteMessage.userIdentityNumber.toLong().rem(colors.size).toInt()])
        when {
            quoteMessage.type.endsWith("_TEXT") -> {
                itemView.reply_content_tv.text = quoteMessage.content
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
                setIcon()
            }
            quoteMessage.type.endsWith("_IMAGE") -> {
                itemView.reply_iv.loadImageCenterCrop(quoteMessage.mediaUrl, R.drawable.image_holder)
                itemView.reply_content_tv.setText(R.string.photo)
                setIcon(R.drawable.ic_status_pic)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
            }
            quoteMessage.type.endsWith("_VIDEO") -> {
                itemView.reply_iv.loadImageCenterCrop(quoteMessage.mediaUrl, R.drawable.image_holder)
                itemView.reply_content_tv.setText(R.string.video)
                setIcon(R.drawable.ic_status_video)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
            }
            quoteMessage.type.endsWith("_DATA") -> {
                notNullElse(quoteMessage.mediaName, {
                    itemView.reply_content_tv.text = it
                }, {
                    itemView.reply_content_tv.setText(R.string.document)
                })
                setIcon(R.drawable.ic_status_file)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
            }
            quoteMessage.type.endsWith("_AUDIO") -> {
                notNullElse(quoteMessage.mediaDuration, {
                    itemView.reply_content_tv.text = it.toLong().formatMillis()
                }, {
                    itemView.reply_content_tv.setText(R.string.audio)
                })
                setIcon(R.drawable.ic_status_audio)
                itemView.reply_iv.visibility = View.GONE
                itemView.reply_avatar.visibility = View.GONE
            }
            quoteMessage.type.endsWith("_STICKER") -> {
                itemView.reply_content_tv.setText(R.string.conversation_status_sticker)
                setIcon(R.drawable.ic_status_stiker)
                itemView.reply_iv.loadImageCenterCrop(quoteMessage.assetUrl, R.drawable.image_holder)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
            }
            quoteMessage.type.endsWith("_CONTACT") -> {
                itemView.reply_content_tv.setText(R.string.contact_less_title)
                setIcon(R.drawable.ic_status_contact)
                itemView.reply_avatar.setInfo(if (quoteMessage.sharedUserFullName != null && quoteMessage.sharedUserFullName.isNotEmpty())
                    quoteMessage.sharedUserFullName[0] else ' ', quoteMessage.sharedUserAvatarUrl, quoteMessage.sharedUserIdentityNumber
                    ?: "0")
                itemView.reply_avatar.visibility = View.VISIBLE
                itemView.reply_iv.visibility = View.INVISIBLE
            }
            quoteMessage.type == MessageCategory.APP_BUTTON_GROUP.name || quoteMessage.type == MessageCategory.APP_CARD.name -> {
                itemView.reply_content_tv.setText(R.string.extensions)
                setIcon(R.drawable.ic_touch_app)
                itemView.reply_iv.visibility = View.VISIBLE
                itemView.reply_avatar.visibility = View.GONE
            }
            else -> {
                itemView.reply_iv.visibility = View.GONE
            }
        }
        chatLayout(isMe, isLast)
    }

    private fun setIcon(@DrawableRes icon: Int? = null) {
        notNullElse(icon, {
            AppCompatResources.getDrawable(itemView.context, it).let {
                it?.setBounds(0, 0, itemView.context.dpToPx(12f), itemView.context.dpToPx(12f))
                TextViewCompat.setCompoundDrawablesRelative(itemView.reply_content_tv, it, null, null, null)
            }
        }, {
            TextViewCompat.setCompoundDrawablesRelative(itemView.reply_content_tv, null, null, null, null)
        })
    }
}