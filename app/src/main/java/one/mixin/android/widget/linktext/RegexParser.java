package one.mixin.android.widget.linktext;

import android.util.Patterns;


class RegexParser {

    static final String PHONE_PATTERN = "[+]{0,1}[(]{0,1}[0-9]{1,3}[)]{0,1}[-0-9]*";
    static final String DECIMAL_PATTERN = "([1-9][\\d]*|0)(\\.[\\d]+)?";
    static final String EMAIL_PATTERN = Patterns.EMAIL_ADDRESS.pattern();
    static final String HASHTAG_PATTERN = "(?:^|\\s|$)#[\\p{L}0-9_]*";
    static final String MENTION_PATTERN = "@(\\S|\\b)+(?:\\s|$)";
    static final String URL_PATTERN = "\\b[a-zA-z+]+:(?://)?[\\w-]+(?:\\.[\\w-]+)*(?:[\\w.,@?^=%&:/~+#-]*[\\w@?^=%&/~+#-])?\\b/?";
    static final String BOT_PATTERN = "(?<=^|\\D)(7000|7000\\d{6})(?=$|\\D)";
}
