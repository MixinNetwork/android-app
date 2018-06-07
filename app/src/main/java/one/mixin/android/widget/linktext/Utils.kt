package one.mixin.android.widget.linktext

import timber.log.Timber
import java.util.regex.Pattern

internal object Utils {

    private fun isValidRegex(regex: String?): Boolean {
        return regex != null && !regex.isEmpty() && regex.length > 2
    }

    fun getRegexByAutoLinkMode(anAutoLinkMode: AutoLinkMode, customRegex: String?): String? {
        when (anAutoLinkMode) {
            AutoLinkMode.MODE_HASHTAG -> return RegexParser.HASHTAG_PATTERN
            AutoLinkMode.MODE_MENTION -> return RegexParser.MENTION_PATTERN
            AutoLinkMode.MODE_URL -> return RegexParser.URL_PATTERN
            AutoLinkMode.MODE_PHONE -> return RegexParser.PHONE_PATTERN
            AutoLinkMode.MODE_EMAIL -> return RegexParser.EMAIL_PATTERN
            AutoLinkMode.MODE_ACCOUNT -> return RegexParser.ACCOUNT_PATTERN
            AutoLinkMode.MODE_CUSTOM -> if (!Utils.isValidRegex(customRegex)) {
                Timber.e("Your custom regex is null, returning URL_PATTERN")
                return RegexParser.URL_PATTERN
            } else {
                return customRegex
            }
            else -> return RegexParser.URL_PATTERN
        }
    }
}

fun getTagData(text: String): Pair<String, String>? {
    val regex = Utils.getRegexByAutoLinkMode(AutoLinkMode.MODE_ACCOUNT, "")
    val pattern = Pattern.compile(regex)
    val matcher = pattern.matcher(text)
    var autoLinkMode: AutoLinkItem? = null
    var name: String? = null
    while (matcher.find()) {
        name = matcher.group(4)
        autoLinkMode = AutoLinkItem(matcher.start(), matcher.end(), matcher.group(), AutoLinkMode.MODE_ACCOUNT)
    }
    if (autoLinkMode != null) {
        val replaceText = text.replaceRange(autoLinkMode.startPoint, autoLinkMode.endPoint, "")
        return Pair(name!!, replaceText)
    }
    return null
}
