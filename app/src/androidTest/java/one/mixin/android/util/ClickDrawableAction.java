package one.mixin.android.util;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.IntDef;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.matcher.BoundedMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static org.hamcrest.core.AllOf.allOf;

public class ClickDrawableAction implements ViewAction
{
    public static final int Left = 0;
    public static final int Top = 1;
    public static final int Right = 2;
    public static final int Bottom = 3;

    @Location
    private final int drawableLocation;

    public ClickDrawableAction(@Location int drawableLocation)
    {
        this.drawableLocation = drawableLocation;
    }

    @Override
    public Matcher<View> getConstraints()
    {
        return allOf(isAssignableFrom(TextView.class), new BoundedMatcher<View, TextView>(TextView.class)
        {
            @Override
            protected boolean matchesSafely(final TextView tv)
            {
                //get focus so drawables are visible and if the textview has a drawable in the position then return a match
                return tv.requestFocusFromTouch() && tv.getCompoundDrawables()[drawableLocation] != null;

            }

            @Override
            public void describeTo(Description description)
            {
                description.appendText("has drawable");
            }
        });
    }

    @Override
    public String getDescription()
    {
        return "click drawable ";
    }

    @Override
    public void perform(final UiController uiController, final View view)
    {
        TextView tv = (TextView)view;//we matched
        if(tv != null && tv.requestFocusFromTouch())//get focus so drawables are visible
        {
            //get the bounds of the drawable image
            Rect drawableBounds = tv.getCompoundDrawables()[drawableLocation].getBounds();

            //calculate the drawable click location for left, top, right, bottom
            final Point[] clickPoint = new Point[4];
            clickPoint[Left] = new Point(tv.getLeft() + (drawableBounds.width() / 2), (int)(tv.getPivotY() + (drawableBounds.height() / 2)));
            clickPoint[Top] = new Point((int)(tv.getPivotX() + (drawableBounds.width() / 2)), tv.getTop() + (drawableBounds.height() / 2));
            clickPoint[Right] = new Point(tv.getRight() + (drawableBounds.width() / 2), (int)(tv.getPivotY() + (drawableBounds.height() / 2)));
            clickPoint[Bottom] = new Point((int)(tv.getPivotX() + (drawableBounds.width() / 2)), tv.getBottom() + (drawableBounds.height() / 2));

            if(tv.dispatchTouchEvent(MotionEvent.obtain(android.os.SystemClock.uptimeMillis(), android.os.SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, clickPoint[drawableLocation].x, clickPoint[drawableLocation].y, 0)))
                tv.dispatchTouchEvent(MotionEvent.obtain(android.os.SystemClock.uptimeMillis(), android.os.SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, clickPoint[drawableLocation].x, clickPoint[drawableLocation].y, 0));
        }
    }

    @IntDef({ Left, Top, Right, Bottom })
    @Retention(RetentionPolicy.SOURCE)
    public @interface Location{}
}