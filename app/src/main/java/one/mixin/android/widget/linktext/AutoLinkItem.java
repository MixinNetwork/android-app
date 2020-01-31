package one.mixin.android.widget.linktext;


class AutoLinkItem {

    private AutoLinkMode autoLinkMode;

    private String matchedText;

    private int startPoint, endPoint;

    private int index;

    AutoLinkItem(int startPoint, int endPoint, String matchedText, AutoLinkMode autoLinkMode) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.matchedText = matchedText;
        this.autoLinkMode = autoLinkMode;
    }

    AutoLinkItem(int startPoint, int endPoint, String matchedText, AutoLinkMode autoLinkMode, int index) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.matchedText = matchedText;
        this.autoLinkMode = autoLinkMode;
        this.index = index;
    }

    AutoLinkMode getAutoLinkMode() {
        return autoLinkMode;
    }

    String getMatchedText() {
        return matchedText;
    }

    int getStartPoint() {
        return startPoint;
    }

    int getEndPoint() {
        return endPoint;
    }

    int getIndex(){ return index; }
}
