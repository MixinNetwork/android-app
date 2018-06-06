package one.mixin.android.ui.conversation.holder

import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.view.View
import kotlinx.android.synthetic.main.date_wrapper.view.*
import kotlinx.android.synthetic.main.item_chat_action.view.chat_name
import kotlinx.android.synthetic.main.item_chat_message.view.*
import one.mixin.android.R
import one.mixin.android.extension.maxItemWidth
import one.mixin.android.extension.notNullElse
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.linktext.AutoLinkMode
import org.jetbrains.anko.dip

class MessageHolder constructor(containerView: View) : BaseViewHolder(containerView) {

    init {
        itemView.chat_tv.addAutoLinkMode(AutoLinkMode.MODE_URL)
        itemView.chat_tv.setUrlModeColor(LINK_COLOR)
        itemView.chat_tv.setAccountModeColor(LINK_COLOR)

        (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams).also {
            it.matchConstraintMaxWidth = itemView.context.maxItemWidth()
        }

        itemView.chat_tv.setAutoLinkOnClickListener { autoLinkMode, matchedText ->
            when (autoLinkMode) {
                AutoLinkMode.MODE_URL -> {
                    onItemListener?.onUrlClick(matchedText)
                }
                AutoLinkMode.MODE_MENTION -> {
                    onItemListener?.onMentionClick(matchedText)
                }
                AutoLinkMode.MODE_ACCOUNT -> {
                    onItemListener?.onUrlClick(matchedText)
                }
                else -> {
                }
            }
        }
    }

    override fun chatLayout(isMe: Boolean, isLast: Boolean) {
        val lp = (itemView.chat_layout.layoutParams as ConstraintLayout.LayoutParams)
        if (isMe) {
            lp.horizontalBias = 1f
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_me_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_me)
            }
        } else {
            lp.horizontalBias = 0f
            if (isLast) {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other_last)
            } else {
                itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
            }
        }
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
        if (messageItem.isBot()) {
            itemView.chat_tv.supportAccount(true)
        } else {
            itemView.chat_tv.supportAccount(false)
        }
        if (hasSelect && isSelect) {
            itemView.setBackgroundColor(Color.parseColor("#660D94FC"))
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
        itemView.chat_time.timeAgoClock(messageItem.createdAt)
        setStatusIcon(isMe, messageItem.status, {
            itemView.chat_flag.setImageDrawable(it)
            itemView.chat_flag.visibility = View.VISIBLE
        }, {
            itemView.chat_flag.visibility = View.GONE
        })

        itemView.setOnClickListener {
            if (hasSelect) {
                onItemListener.onSelect(!isSelect, messageItem, adapterPosition)
            }
        }

        chatLayout(isMe, isLast)
    }
}