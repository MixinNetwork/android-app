package one.mixin.android.widget.linktext

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.BackgroundColorSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.emoji2.widget.EmojiTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import one.mixin.android.Constants.Colors.HIGHLIGHTED
import one.mixin.android.extension.clickVibrate
import one.mixin.android.util.markdown.MarkwonUtil.Companion.simpleMarkwon
import one.mixin.android.util.mention.MentionRenderContext
import one.mixin.android.util.mention.mentionNumberPattern
import one.mixin.android.widget.linktext.RegexParser.DECIMAL_PATTERN
import org.commonmark.node.Node
import java.util.LinkedList
import java.util.regex.Pattern

open class AutoLinkTextView(context: Context, attrs: AttributeSet?) :
    EmojiTextView(context, attrs) {

    private var coroutineScope: CoroutineScope? = null
    private var autoLinkOnClickListener: ((AutoLinkMode, String) -> Unit)? = null
    private var autoLinkOnLongClickListener: ((AutoLinkMode, String) -> Unit)? = null

    private var autoLinkModes: Array<out AutoLinkMode>? = null

    private var customRegex: String? = null

    private var isUnderLineEnabled = false

    private var mentionModeColor = DEFAULT_COLOR
    private var hashtagModeColor = DEFAULT_COLOR
    private var urlModeColor = DEFAULT_COLOR
    private var phoneModeColor = DEFAULT_COLOR
    private var emailModeColor = DEFAULT_COLOR
    private var customModeColor = DEFAULT_COLOR
    private var botModeColor = DEFAULT_COLOR
    private var defaultSelectedColor = Color.LTGRAY
    var clickTime: Long = 0
    var mentionRenderContext: MentionRenderContext? = null
    var keyWord: String? = null
        set(value) {
            if (field != value) {
                field = value
            }
        }

    override fun setText(text: CharSequence, type: TextView.BufferType) {
        if (TextUtils.isEmpty(text)) {
            super.setText(text, type)
            return
        }
        val autoLinkItems = LinkedList<AutoLinkItem>()
        val sp = SpannableStringBuilder()
        sp.append(SpannableString(text))
        renderMention(sp, autoLinkItems)
        renderKeyWord(sp)
        matchedRanges(sp, autoLinkItems)
        if (movementMethod == null) {
            movementMethod = LinkTouchMovementMethod()
        }
        super.setText(makeSpannableString(sp, autoLinkItems), type)
    }

    private fun renderMarkdown(sp: SpannableStringBuilder, node: Node) {
        sp.append(simpleMarkwon.render(node))
        if (node.next != null) {
            renderMarkdown(sp, node.next)
        }
    }

    private fun renderKeyWord(sp: SpannableStringBuilder) {
        keyWord?.let { keyWord ->
            val start = sp.indexOf(keyWord, 0, true)
            if (start >= 0) {
                sp.setSpan(
                    BackgroundColorSpan(HIGHLIGHTED),
                    start,
                    start + keyWord.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }
    }

    private fun renderMention(
        text: SpannableStringBuilder,
        autoLinkItems: LinkedList<AutoLinkItem>
    ): CharSequence {
        val mentionRenderContext = this.mentionRenderContext ?: return text
        val matcher = mentionNumberPattern.matcher(text)
        var offset = 0
        while (matcher.find()) {
            val name = mentionRenderContext.userMap[matcher.group().substring(1)] ?: continue
            text.replace(matcher.start() + 1 - offset, matcher.end() - offset, name)
            autoLinkItems.add(
                AutoLinkItem(
                    matcher.start() - offset,
                    matcher.start() + name.length - offset + 1,
                    matcher.group(),
                    AutoLinkMode.MODE_MENTION
                )
            )
            offset += ((matcher.group().length - 1) - name.length)
        }
        return text
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        coroutineScope = MainScope()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        coroutineScope?.cancel()
    }

    override fun performLongClick(): Boolean {
        return if (handleLongClick) {
            handleLongClick = false
            true
        } else {
            super.performLongClick()
        }
    }

    private var handleLongClick = false

    private fun makeSpannableString(
        text: CharSequence,
        autoLinkItems: List<AutoLinkItem>
    ): SpannableString {
        val spannableString = if (text is SpannableString) {
            text
        } else {
            SpannableString(text)
        }

        for (autoLinkItem in autoLinkItems) {
            val currentColor = getColorByMode(autoLinkItem.autoLinkMode)

            val clickableSpan = if (autoLinkItem.autoLinkMode == AutoLinkMode.MODE_URL) {
                object : LongTouchableSpan(currentColor, defaultSelectedColor, isUnderLineEnabled) {
                    var job: Job? = null
                    override fun onClick(widget: View) {
                        if (isLongPressed) return
                        autoLinkOnClickListener?.let {
                            it(autoLinkItem.autoLinkMode, autoLinkItem.matchedText)
                        }
                    }

                    override fun startLongClick() {
                        job = coroutineScope?.launch {
                            setLongPressed(false)
                            handleLongClick = false
                            delay(LONG_CLICK_TIME)
                            autoLinkOnLongClickListener?.let {
                                context.clickVibrate()
                                it(autoLinkItem.autoLinkMode, autoLinkItem.matchedText)
                            }
                            setLongPressed(true)
                            handleLongClick = true
                        }
                    }

                    override fun cancelLongClick(): Boolean {
                        return if (isLongPressed) {
                            false
                        } else {
                            if (job?.isActive == true) {
                                job?.cancel()
                                setLongPressed(false)
                                handleLongClick = false
                            }
                            true
                        }
                    }

                    override fun updateDrawState(textPaint: TextPaint) {
                        super.updateDrawState(textPaint)
                        val textColor = normalTextColor
                        textPaint.color = textColor
                        textPaint.bgColor = if (isPressed) pressedTextColor else Color.TRANSPARENT
                        textPaint.isUnderlineText = isUnderLineEnabled
                    }
                }
            } else {
                object : TouchableSpan(currentColor, defaultSelectedColor, isUnderLineEnabled) {
                    override fun onClick(widget: View) {
                        autoLinkOnClickListener?.let {
                            it(autoLinkItem.autoLinkMode, autoLinkItem.matchedText)
                        }
                    }

                    override fun updateDrawState(textPaint: TextPaint) {
                        super.updateDrawState(textPaint)
                        val textColor = normalTextColor
                        textPaint.color = textColor
                        textPaint.bgColor = if (isPressed) pressedTextColor else Color.TRANSPARENT
                        textPaint.isUnderlineText = isUnderLineEnabled
                    }
                }
            }

            spannableString.setSpan(
                clickableSpan,
                autoLinkItem.startPoint,
                autoLinkItem.endPoint,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return spannableString
    }

    private fun matchedRanges(
        text: CharSequence,
        autoLinkItems: MutableList<AutoLinkItem>
    ): List<AutoLinkItem> {
        for (anAutoLinkMode in requireNotNull(autoLinkModes)) {
            val regex = Utils.getRegexByAutoLinkMode(anAutoLinkMode, customRegex)
            val pattern = Pattern.compile(regex)
            val matcher = pattern.matcher(text)

            when {
                anAutoLinkMode == AutoLinkMode.MODE_EMAIL || anAutoLinkMode == AutoLinkMode.MODE_URL -> {
                    while (matcher.find()) {
                        val item = AutoLinkItem(matcher.start(), matcher.end(), matcher.group(), anAutoLinkMode)
                        if (!autoLinkItems.any { item.startPoint >= it.startPoint && item.endPoint <= it.endPoint }) {
                            // Remove link contained by url
                            autoLinkItems.removeAll(autoLinkItems.filter { it.autoLinkMode != AutoLinkMode.MODE_MENTION && it.startPoint >= matcher.start() && it.endPoint <= matcher.end() })
                            autoLinkItems.add(item)
                        }
                    }
                }
                anAutoLinkMode == AutoLinkMode.MODE_PHONE -> {
                    while (matcher.find()) {
                        if (anAutoLinkMode != AutoLinkMode.MODE_PHONE || matcher.group().length > MIN_PHONE_NUMBER_LENGTH) {
                            addLinkItems(autoLinkItems, AutoLinkItem(matcher.start(), matcher.end(), matcher.group(), anAutoLinkMode))
                        }
                    }
                    val deciPattern = Pattern.compile(DECIMAL_PATTERN)
                    val deciMatcher = deciPattern.matcher(text)
                    while (deciMatcher.find()) {
                        // Culling decimals
                        autoLinkItems.removeAll(autoLinkItems.filter { it.autoLinkMode == AutoLinkMode.MODE_PHONE && it.startPoint >= deciMatcher.start() && it.endPoint <= deciMatcher.end() })
                    }
                }
                anAutoLinkMode != AutoLinkMode.MODE_MENTION -> {
                    while (matcher.find()) {
                        addLinkItems(autoLinkItems, AutoLinkItem(matcher.start(), matcher.end(), matcher.group(), anAutoLinkMode))
                    }
                }
            }
        }

        return autoLinkItems
    }

    private fun addLinkItems(autoLinkItems: MutableList<AutoLinkItem>, item: AutoLinkItem) {
        // Skip if the current link is contain
        if (!autoLinkItems.any { item.startPoint >= it.startPoint && item.endPoint <= it.endPoint }) {
            autoLinkItems.add(item)
        }
    }

    private fun getColorByMode(autoLinkMode: AutoLinkMode): Int {
        return when (autoLinkMode) {
            AutoLinkMode.MODE_HASHTAG -> hashtagModeColor
            AutoLinkMode.MODE_MENTION -> mentionModeColor
            AutoLinkMode.MODE_URL -> urlModeColor
            AutoLinkMode.MODE_PHONE -> phoneModeColor
            AutoLinkMode.MODE_EMAIL -> emailModeColor
            AutoLinkMode.MODE_BOT -> botModeColor
            AutoLinkMode.MODE_CUSTOM -> customModeColor
        }
    }

    fun setMentionModeColor(@ColorInt mentionModeColor: Int) {
        this.mentionModeColor = mentionModeColor
    }

    fun setHashtagModeColor(@ColorInt hashtagModeColor: Int) {
        this.hashtagModeColor = hashtagModeColor
    }

    fun setUrlModeColor(@ColorInt urlModeColor: Int) {
        this.urlModeColor = urlModeColor
    }

    fun setPhoneModeColor(@ColorInt phoneModeColor: Int) {
        this.phoneModeColor = phoneModeColor
    }

    fun setEmailModeColor(@ColorInt emailModeColor: Int) {
        this.emailModeColor = emailModeColor
    }

    fun setBotModeColor(@ColorInt botModeColor: Int) {
        this.botModeColor = botModeColor
    }

    fun setCustomModeColor(@ColorInt customModeColor: Int) {
        this.customModeColor = customModeColor
    }

    fun setSelectedStateColor(@ColorInt defaultSelectedColor: Int) {
        this.defaultSelectedColor = defaultSelectedColor
    }

    fun addAutoLinkMode(vararg autoLinkModes: AutoLinkMode) {
        this.autoLinkModes = autoLinkModes
    }

    fun setCustomRegex(regex: String) {
        this.customRegex = regex
    }

    fun setAutoLinkOnClickListener(autoLinkOnClickListener: (AutoLinkMode, String) -> Unit) {
        this.autoLinkOnClickListener = autoLinkOnClickListener
    }

    fun setAutoLinkOnLongClickListener(autoLinkOnLongClickListener: (AutoLinkMode, String) -> Unit) {
        this.autoLinkOnLongClickListener = autoLinkOnLongClickListener
    }

    fun enableUnderLine() {
        isUnderLineEnabled = true
    }

    companion object {

        internal val TAG = AutoLinkTextView::class.java.simpleName

        private const val MIN_PHONE_NUMBER_LENGTH = 6

        private const val DEFAULT_COLOR = Color.RED

        private const val LONG_CLICK_TIME = 200L
    }
}
