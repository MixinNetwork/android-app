package one.mixin.android.extension

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.ui.conversation.adapter.MessageAdapter
import one.mixin.android.util.mention.MentionRenderContext
import one.mixin.android.util.mention.MentionTextView
import one.mixin.android.widget.NoUnderLineSpan
import one.mixin.android.widget.linktext.AutoLinkMode
import one.mixin.android.widget.linktext.AutoLinkTextView

fun TextView.highlightStarTag(
    source: String,
    links: Array<String>,
    @ColorInt color: Int = ContextCompat.getColor(context, R.color.colorBlue),
    onItemListener: MessageAdapter.OnItemListener? = null,
) {
    val spannableStringBuilder =
        try {
            var start: Int
            var end: Int
            val stringBuilder = StringBuilder(source)
            val targets = arrayListOf<Int>()
            while (stringBuilder.indexOf("**").also { start = it } != -1) {
                stringBuilder.replace(start, start + 2, "")
                end = stringBuilder.indexOf("**")
                if (end >= 0) {
                    stringBuilder.replace(end, end + 2, "")
                    targets.add(start)
                    targets.add(end)
                }
            }

            val spannableStringBuilder = SpannableStringBuilder(stringBuilder)
            for (i in 0 until targets.count() / 2) {
                spannableStringBuilder.setSpan(NoUnderLineSpan(links[i], onItemListener), targets[i * 2], targets[i * 2 + 1], Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                spannableStringBuilder.setSpan(ForegroundColorSpan(color), targets[i * 2], targets[i * 2 + 1], Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            }
            spannableStringBuilder
        } catch (e: Exception) {
            SpannableStringBuilder(source)
        }

    text = spannableStringBuilder
    movementMethod = LinkMovementMethod.getInstance()
}

fun TextView.highlightLinkText(
    source: String,
    texts: Array<String>,
    links: Array<String>,
    color: Int = ContextCompat.getColor(context, R.color.colorBlue),
    onItemListener: MessageAdapter.OnItemListener? = null,
) {
    require(texts.size == links.size) { "texts's length should equals with links" }
    val sp = SpannableString(source)
    for (i in texts.indices) {
        val text = texts[i]
        val link = links[i]
        val start = source.indexOf(text, ignoreCase = true)
        require(start != -1) { "start index can not be -1" }
        sp.setSpan(
            NoUnderLineSpan(link, onItemListener),
            start,
            start + text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        sp.setSpan(
            ForegroundColorSpan(color),
            start,
            start + text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
    }
    text = sp
    movementMethod = LinkMovementMethod.getInstance()
}

fun TextView.highLight(
    target: String?,
    ignoreCase: Boolean = true,
    @ColorInt color: Int = resources.getColor(R.color.wallet_blue_secondary, null),
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
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        index = text.indexOf(target, index + target.length, ignoreCase = ignoreCase)
    }
    setText(spannable)
}

fun TextView.highLightClick(
    target: String?,
    ignoreCase: Boolean = true,
    @ColorInt color: Int = resources.getColor(R.color.wallet_blue_secondary, null),
    action: () -> Unit,
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
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
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
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        index = text.indexOf(target, index + target.length, ignoreCase = ignoreCase)
    }
    setText(spannable)
}

fun TextView.bold(target: String) {
    val text = this.text.toString()
    val spannableString = SpannableString(text)
    val startIndex = text.indexOf(target)
    val endIndex = startIndex + target.length
    val boldSpan = StyleSpan(Typeface.BOLD)
    spannableString.setSpan(boldSpan, startIndex, endIndex, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    setText(spannableString)
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

fun TextView.timeAgoDay(
    str: String,
    pattern: String = "dd/MM/yyyy",
) {
    text = str.timeAgoDay(pattern)
}

fun MentionTextView.renderMessage(
    text: CharSequence?,
    mentionRenderContext: MentionRenderContext?,
) {
    if (text == null || mentionRenderContext == null) {
        this.text = text
        return
    }
    this.mentionRenderContext = mentionRenderContext
    this.text = text
}

fun AutoLinkTextView.renderMessage(
    text: CharSequence?,
    keyWord: String? = null,
    mentionRenderContext: MentionRenderContext? = null,
) {
    this.mentionRenderContext = mentionRenderContext
    this.keyWord = keyWord
    this.text = text
}

fun EditText.clearCharacterStyle() {
    editableText?.let { string ->
        val toBeRemovedSpans = string.getSpans(0, string.length, CharacterStyle::class.java)
        if (toBeRemovedSpans.isNotEmpty()) {
            for (span in toBeRemovedSpans) {
                string.removeSpan(span)
            }
            val curString = string.trim()
            setText(curString)
            setSelection(curString.length)
        }
    }
}

fun AutoLinkTextView.initChatMode(
    @ColorInt linkColor: Int,
) {
    addAutoLinkMode(AutoLinkMode.MODE_BOT, AutoLinkMode.MODE_EMAIL, AutoLinkMode.MODE_URL)
    setUrlModeColor(linkColor)
    setMentionModeColor(linkColor)
    setBotModeColor(linkColor)
    setEmailModeColor(linkColor)
    setPhoneModeColor(linkColor)
}

var TextView.textColor: Int
    @Deprecated("Property does not have a getter")
    get() = error("Property does not have a getter")
    set(v) = setTextColor(v)

var TextView.textColorResource: Int
    @Deprecated("Property does not have a getter")
    get() = error("Property does not have a getter")
    set(colorId) = setTextColor(context.resources.getColor(colorId))

var TextView.textResource: Int
    @Deprecated("Property does not have a getter")
    get() = error("Property does not have a getter")
    set(v) = setText(v)

var TextView.hintTextColor: Int
    @Deprecated("Property does not have a getter")
    get() = error("Property does not have a getter")
    set(v) = setHintTextColor(v)

private val walletGreen = MixinApplication.appContext.getColor(R.color.wallet_green)
private val walletRed = MixinApplication.appContext.getColor(R.color.wallet_pink)
private val walletGray = MixinApplication.appContext.getColor(R.color.wallet_text_gray)

fun TextView.setQuoteText(text: String?, isRising: Boolean?) {
    val quoteColorPref = context.defaultSharedPreferences
        .getBoolean(Constants.Account.PREF_QUOTE_COLOR, false)

    val color = when {
        text == null -> walletGray
        isRising == true -> if (quoteColorPref) walletRed else walletGreen
        isRising == false -> if (quoteColorPref) walletGreen else walletRed
        else -> walletGray
    }

    this.text = text
    setTextColor(color)
}