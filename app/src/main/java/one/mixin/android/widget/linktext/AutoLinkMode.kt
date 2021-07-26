package one.mixin.android.widget.linktext

enum class AutoLinkMode(name: String) {
    MODE_HASHTAG("Hashtag"),
    MODE_MENTION("Mention"),
    MODE_URL("Url"),
    MODE_PHONE("Phone"),
    MODE_EMAIL("Email"),
    MODE_BOT("Bot"),
    MODE_CUSTOM("Custom"),
    MODE_MARKDOWN_BOLD("Bold"),
    MODE_MARKDOWN_ITALIC("Slant"),
    MODE_MARKDOWN_STRIKETHROUGH("Strikethrough"),
    MODE_MARKDOWN_INLINE("inline");

    override fun toString(): String {
        return name
    }
}
