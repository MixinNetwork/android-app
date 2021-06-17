package one.mixin.android.widget.linktext;


public enum AutoLinkMode {

    MODE_HASHTAG("Hashtag"),
    MODE_MENTION("Mention"),
    MODE_URL("Url"),
    MODE_PHONE("Phone"),
    MODE_EMAIL("Email"),
    MODE_BOT("BOT"),
    MODE_CUSTOM("Custom");

    private String name;

    AutoLinkMode(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
