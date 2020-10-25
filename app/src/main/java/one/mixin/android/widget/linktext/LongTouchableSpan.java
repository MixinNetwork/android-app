package one.mixin.android.widget.linktext;

import android.view.View;
import androidx.annotation.NonNull;

public abstract class LongTouchableSpan extends TouchableSpan {

    protected boolean isPressed;
    protected boolean isLongPressed;

    protected LongTouchableSpan(int normalTextColor, int pressedTextColor, boolean isUnderLineEnabled) {
        super(normalTextColor, pressedTextColor, isUnderLineEnabled);
    }

    void setLongPressed(boolean isSelected) {
        isLongPressed = isSelected;
    }

    public abstract void onClick(@NonNull View widget);
    public abstract void startLongClick();
    public abstract boolean cancelLongClick();
}