package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

open class DotTextLayout : ViewGroup {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    )

    private var offset = 0

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val childCount = childCount
        if (childCount < 2) {
            throw RuntimeException("CustomLayout child count must >= 2")
        }
        if (getChildAt(1) !is TextView) {
            throw RuntimeException("CustomLayout second child view not a TextView")
        }
        val paddingWidth = paddingStart + paddingEnd
        val paddingHeight = paddingTop + paddingBottom

        val firstView = getChildAt(0)
        val secondView = getChildAt(1) as TextView

        measureChild(
            firstView,
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getMode(widthMeasureSpec)),
            heightMeasureSpec,
        )
        measureChild(
            secondView,
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec) - paddingWidth - firstView.measuredWidth, MeasureSpec.getMode(widthMeasureSpec)),
            heightMeasureSpec,
        )
        initTextParams(secondView, firstView)
        setMeasuredDimension(
            firstView.measuredWidth + secondView.measuredWidth + paddingWidth,
            secondView.measuredHeight + paddingHeight,
        )
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        val firstView = getChildAt(0)
        val secondView = getChildAt(1)

        firstView.layout(
            paddingLeft,
            paddingTop + offset,
            paddingLeft + firstView.measuredWidth,
            paddingTop + offset + firstView.measuredHeight,
        )
        secondView.layout(
            paddingLeft + firstView.measuredWidth,
            paddingTop,
            paddingLeft + firstView.measuredWidth + secondView.measuredWidth,
            paddingTop + secondView.measuredHeight,
        )
    }

    private fun initTextParams(
        textView: TextView,
        view: View,
    ) {
        offset = (textView.lineHeight - view.measuredHeight) / 2
    }

    override fun generateDefaultLayoutParams(): MarginLayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(p: LayoutParams): MarginLayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet): MarginLayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}
