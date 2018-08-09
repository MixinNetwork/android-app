package one.mixin.android.ui.conversation.holder

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.View
import kotlinx.android.synthetic.main.item_chat_waiting.view.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.timeAgoClock
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.NoUnderLineSpan
import org.jetbrains.anko.dip

class WaitingHolder constructor(
    containerView: View,
    private val onItemListener: ConversationAdapter.OnItemListener
) : BaseViewHolder(containerView) {

    override fun chatLayout(isMe: Boolean, isLast: Boolean, isBlink: Boolean) {
        super.chatLayout(isMe, isLast, isBlink)
        if (isLast) {
            itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other_last)
        } else {
            itemView.chat_layout.setBackgroundResource(R.drawable.chat_bubble_other)
        }
    }

    init {
        itemView.chat_tv.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun highlightLinkText(
        source: String,
        color: Int,
        texts: Array<String>,
        links: Array<String>
    ): SpannableString {
        if (texts.size != links.size) {
            throw IllegalArgumentException("texts's length should equals with links")
        }
        val sp = SpannableString(source)
        for (i in texts.indices) {
            val text = texts[i]
            val link = links[i]
            val start = source.indexOf(text)
            sp.setSpan(NoUnderLineSpan(link, onItemListener), start,
                start + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(ForegroundColorSpan(color), start, start + text.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return sp
    }

    fun bind(
        messageItem: MessageItem,
        isLast: Boolean,
        isFirst: Boolean,
        onItemListener: ConversationAdapter.OnItemListener
    ) {
        itemView.chat_time.timeAgoClock(messageItem.createdAt)

        val colorPrimary = MixinApplication.get().getColor(R.color.colorBlue)
        val learn: String = MixinApplication.get().getString(R.string.chat_learn)
        val info = MixinApplication.get().getString(R.string.chat_waiting, messageItem.userFullName, learn)
        val learnUrl = MixinApplication.get().getString(R.string.chat_waiting_url)
        itemView.chat_tv.text = highlightLinkText(
            info,
            colorPrimary,
            arrayOf(learn),
            arrayOf(learnUrl))

        if (isFirst) {
            itemView.chat_name.visibility = View.VISIBLE
            itemView.chat_name.text = messageItem.userFullName
            if (messageItem.appId != null) {
                itemView.chat_name.setCompoundDrawables(null, null, botIcon, null)
                itemView.chat_name.compoundDrawablePadding = itemView.dip(3)
            } else {
                itemView.chat_name.setCompoundDrawables(null, null, null, null)
            }
            itemView.chat_name.setOnClickListener { onItemListener.onUserClick(messageItem.userId) }
            itemView.chat_name.setTextColor(colors[messageItem.userIdentityNumber.toLong().rem(colors.size).toInt()])
        } else {
            itemView.chat_name.visibility = View.GONE
        }
        chatLayout(false, isLast)
    }
}