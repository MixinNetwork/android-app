package one.mixin.android.extension

import android.content.res.ColorStateList
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TextAppearanceSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import one.mixin.android.R
import one.mixin.android.ui.conversation.adapter.ConversationAdapter
import one.mixin.android.ui.conversation.holder.BaseViewHolder
import one.mixin.android.util.mention.MentionRenderContext
import one.mixin.android.util.mention.mentionConversationParser
import one.mixin.android.util.mention.syntax.simple.SimpleRenderer
import one.mixin.android.widget.NoUnderLineSpan
import one.mixin.android.widget.linktext.AutoLinkTextView

fun TextView.highlightLinkText(
    source: String,
    texts: Array<String>,
    links: Array<String>,
    color: Int = ContextCompat.getColor(context, R.color.colorBlue),
    onItemListener: ConversationAdapter.OnItemListener? = null
) {
    require(texts.size == links.size) { "texts's length should equals with links" }
    val sp = SpannableString(source)
    for (i in texts.indices) {
        val text = texts[i]
        val link = links[i]
        val start = source.indexOf(text)
        require(start != -1) { "start index can not be -1" }
        sp.setSpan(NoUnderLineSpan(link, onItemListener), start, start + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.setSpan(ForegroundColorSpan(color), start, start + text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    text = sp
    movementMethod = LinkMovementMethod.getInstance()
}

fun TextView.highLight(
    target: String?,
    ignoreCase: Boolean = true,
    @ColorInt color: Int = resources.getColor(R.color.wallet_blue_secondary, null)
) {
    if (target.isNullOrBlank()) {
        return
    }
    val text = this.text.toString()
    val spannable = SpannableString(text)
    var index = text.indexOf(target, ignoreCase = ignoreCase)
    while (index != -1) {
        spannable.setSpan(
            TextAppearanceSpan(null, 0, 0, ColorStateList.valueOf(color), null),
            index,
            index + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        index = text.indexOf(target, index + target.length, ignoreCase = ignoreCase)
    }
    setText(spannable)
}

fun TextView.highLightClick(
    target: String?,
    ignoreCase: Boolean = true,
    @ColorInt color: Int = resources.getColor(R.color.wallet_blue_secondary, null),
    action: () -> Unit
) {
    if (target.isNullOrBlank()) {
        return
    }
    val text = this.text.toString()
    val spannable = SpannableString(text)
    var index = text.indexOf(target, ignoreCase = ignoreCase)
    while (index != -1) {
        spannable.setSpan(
            ForegroundColorSpan(color),
            index,
            index + target.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannable.setSpan(
            object : ClickableSpan() {
                override fun onClick(widget: View) {
                    action()
                }

                override fun updateDrawState(ds: TextPaint) {
                    ds.color = ds.linkColor
                    ds.isUnderlineText = false
                }
            },
            index,
            index + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        index = text.indexOf(target, index + target.length, ignoreCase = ignoreCase)
    }
    setText(spannable)
}

fun TextView.timeAgo(str: String) {
    text = str.timeAgo(context)
}

fun TextView.timeAgoClock(str: String) {
    text = str.timeAgoClock()
}

fun TextView.timeAgoDate(str: String) {
    text = str.timeAgoDate(context)
}

fun TextView.timeAgoDay(str: String, pattern: String = "dd/MM/yyyy") {
    text = str.timeAgoDay(pattern)
}

fun TextView.renderConversation(text: CharSequence?, mentionRenderContext: MentionRenderContext?) {
    if (text == null || mentionRenderContext == null) {
        this.text = text
        return
    }
    this.text = SimpleRenderer.render(
        text,
        parser = mentionConversationParser,
        renderContext = mentionRenderContext
    )
}

fun TextView.renderMessage(text: CharSequence?, mentionRenderContext: MentionRenderContext?, keyWord: String? = null) {
    if (text == null || mentionRenderContext == null) {
        this.text = text
        return
    }
    if (this is AutoLinkTextView) {
        this.mentionRenderContext = mentionRenderContext
    }
    val sp = SpannableString(text)
    if (keyWord != null) {
        val start = sp.indexOf(keyWord, 0, true)
        if (start >= 0) {
            sp.setSpan(
                BackgroundColorSpan(BaseViewHolder.HIGHLIGHTED),
                start,
                start + keyWord.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }
    this.text = sp
}
