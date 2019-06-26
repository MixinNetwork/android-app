package one.mixin.android.widget;

import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.util.IntProperty;
import android.util.Property;
import androidx.annotation.Keep;

public class BackgroundDrawable extends ColorDrawable {

    private boolean allowDrawContent;

    public BackgroundDrawable(int color) {
        super(color);
    }

    @Keep
    @Override
    public void setAlpha(int alpha) {
        super.setAlpha(alpha);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    public static final Property<ColorDrawable, Integer> COLOR_DRAWABLE_ALPHA = new IntProperty<ColorDrawable>("alpha") {
        @Override
        public void setValue(ColorDrawable object, int value) {
            object.setAlpha(value);
        }

        @Override
        public Integer get(ColorDrawable object) {
            return object.getAlpha();
        }
    };
}