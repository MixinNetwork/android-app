package one.mixin.android.widget.linktext;

import android.text.style.ClickableSpan;

public abstract class TouchableSpan extends ClickableSpan {

    protected boolean isPressed;
    protected int normalTextColor;
    protected int pressedTextColor;
    protected boolean isUnderLineEnabled;

    protected TouchableSpan(int normalTextColor, int pressedTextColor, boolean isUnderLineEnabled) {
        this.normalTextColor = normalTextColor;
        this.pressedTextColor = pressedTextColor;
        this.isUnderLineEnabled = isUnderLineEnabled;
    }

    void setPressed(boolean isSelected) {
        isPressed = isSelected;
    }

}