package one.mixin.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class ButtonGroupLayout extends ViewGroup {
    private int lineSpacing;

    public ButtonGroupLayout(Context context) {
        this(context, null);
    }

    public ButtonGroupLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ButtonGroupLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        lineSpacing = 0;
    }

    public void setLineSpacing(int lineSpacing) {
        this.lineSpacing = lineSpacing;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width = 0;
        int height = getPaddingTop() + getPaddingBottom();

        int childCount = getChildCount();
        int maxChildHeight = 0;
        int lineWidth = getPaddingLeft() + getPaddingRight();

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
            lineWidth += child.getMeasuredWidth() + lineSpacing;

            if ((i + 1) % 3 == 0 || i == childCount - 1) {
                height += maxChildHeight;
                lineWidth = getPaddingLeft() + getPaddingRight();
                maxChildHeight = 0;
            }
        }

        setMeasuredDimension(
                widthMode == MeasureSpec.EXACTLY ? widthSize : width,
                heightMode == MeasureSpec.EXACTLY ? heightSize : height
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = getWidth() - getPaddingLeft() - getPaddingRight();
        int childCount = getChildCount();
        int lineWidth = getPaddingLeft();
        int top = getPaddingTop();
        int maxChildHeight = 0;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            int childrenInRow = Math.min(3, childCount - i);

            if (childrenInRow == 1) {
                int childWidth = width;
                child.measure(
                        MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                        MeasureSpec.makeMeasureSpec(child.getMeasuredHeight(), MeasureSpec.EXACTLY)
                );
                child.layout(lineWidth, top, lineWidth + childWidth, top + child.getMeasuredHeight());
            } else if (childrenInRow == 2) {
                int childWidth = (width - lineSpacing) / 2;
                for (int j = 0; j < childrenInRow; j++) {
                    View subChild = getChildAt(i + j);
                    subChild.measure(
                            MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(subChild.getMeasuredHeight(), MeasureSpec.EXACTLY)
                    );
                    subChild.layout(lineWidth, top, lineWidth + childWidth, top + subChild.getMeasuredHeight());
                    lineWidth += childWidth + lineSpacing;
                }
            } else {
                int childWidth = (width - 2 * lineSpacing) / 3;
                for (int j = 0; j < childrenInRow; j++) {
                    View subChild = getChildAt(i + j);
                    subChild.measure(
                            MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(subChild.getMeasuredHeight(), MeasureSpec.EXACTLY)
                    );
                    subChild.layout(lineWidth, top, lineWidth + childWidth, top + subChild.getMeasuredHeight());
                    lineWidth += childWidth + lineSpacing;
                }
            }

            maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
            top += maxChildHeight;
            lineWidth = getPaddingLeft();
            maxChildHeight = 0;
            i += childrenInRow - 1;
        }
    }
}