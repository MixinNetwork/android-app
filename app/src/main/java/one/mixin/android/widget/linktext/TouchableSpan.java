package one.mixin.android.widget.linktext;

import android.text.NoCopySpan;
import android.text.style.ClickableSpan;

public abstract class TouchableSpan extends ClickableSpan
    // https://stackoverflow.com/questions/28539216/android-textview-leaks-with-setmovementmethod/53202503#53202503
    implements NoCopySpan {

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