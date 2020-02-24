package one.mixin.android.widget.linktext;

import android.util.Patterns;


class RegexParser {

    static final String PHONE_PATTERN = Patterns.PHONE.pattern();
    static final String EMAIL_PATTERN = Patterns.EMAIL_ADDRESS.pattern();
    static final String HASHTAG_PATTERN = "(?:^|\\s|$)#[\\p{L}0-9_]*";
    static final String MENTION_PATTERN = "@(\\S|\\b)+(?:\\s|$)";
    static final String URL_PATTERN = "[a-zA-z]+://[^\\s]*(?<!\\))";
    
}
