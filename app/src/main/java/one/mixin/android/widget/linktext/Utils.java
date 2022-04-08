package one.mixin.android.widget.linktext;

import timber.log.Timber;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

class Utils {

    private static boolean isValidRegex(String regex) {
        return regex != null && !regex.isEmpty() && regex.length() > 2;
    }

    private static Map<String, Pattern> petterns = new HashMap<>();

    static Pattern getPatternByAutoLinkMode(AutoLinkMode anAutoLinkMode, String customRegex){
        Pattern p = petterns.get(anAutoLinkMode.name());
        if(p==null){
            p = Pattern.compile(getRegexByAutoLinkMode(anAutoLinkMode,customRegex));
            petterns.put(anAutoLinkMode.name(),p);
        }
        return p;
    }

    private static String getRegexByAutoLinkMode(AutoLinkMode anAutoLinkMode, String customRegex) {
        switch (anAutoLinkMode) {
            case MODE_HASHTAG:
                return RegexParser.HASHTAG_PATTERN;
            case MODE_MENTION:
                return RegexParser.MENTION_PATTERN;
            case MODE_PHONE:
                return RegexParser.PHONE_PATTERN;
            case MODE_EMAIL:
                return RegexParser.EMAIL_PATTERN;
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
