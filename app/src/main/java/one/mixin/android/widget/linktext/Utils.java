package one.mixin.android.widget.linktext;

import timber.log.Timber;

class Utils {

    private static boolean isValidRegex(String regex) {
        return regex != null && !regex.isEmpty() && regex.length() > 2;
    }

    static String getRegexByAutoLinkMode(AutoLinkMode anAutoLinkMode, String customRegex) {
        switch (anAutoLinkMode) {
            case MODE_HASHTAG:
                return RegexParser.HASHTAG_PATTERN;
            case MODE_MENTION:
                return RegexParser.MENTION_PATTERN;
            case MODE_PHONE:
                return RegexParser.PHONE_PATTERN;
            case MODE_EMAIL:
                return RegexParser.EMAIL_PATTERN;
            case MODE_MARKDOWN_BOLD:
                return RegexParser.MARKDOWN_BOLD;
            case MODE_MARKDOWN_ITALIC:
                return RegexParser.MARKDOWN_ITALIC;
            case MODE_MARKDOWN_STRIKETHROUGH:
                return RegexParser.MARKDOWN_STRIKETHROUGH;
            case MODE_MARKDOWN_INLINE:
                return RegexParser.MARKDOWN_INLINE;
            case MODE_BOT:
                return RegexParser.BOT_PATTERN;
            case MODE_CUSTOM:
                if (!Utils.isValidRegex(customRegex)) {
                    Timber.e("Your custom regex is null, returning URL_PATTERN");
                    return RegexParser.URL_PATTERN;
                } else {
                    return customRegex;
                }
            default:
                return RegexParser.URL_PATTERN;
        }
    }

}
