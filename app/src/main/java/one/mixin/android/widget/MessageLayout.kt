package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import one.mixin.android.R
import one.mixin.android.extension.dp
import kotlin.math.max

open class MessageLayout : ViewGroup {
    private val offset: Int = 8.dp
    private var lastLineWidth: Float = 0.toFloat()
    private var isRtl: Boolean = false
    private var maxWidth: Int = 0
    private var contentPadding: Int = 0

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.MessageLayout, defStyleAttr, 0)
        maxWidth = ta.getDimensionPixelSize(R.styleable.MessageLayout_max_width, 300.dp)
        contentPadding = ta.getDimensionPixelSize(R.styleable.MessageLayout_content_padding, 0)
        ta.recycle()
    }

    fun setMaxWidth(maxWidth: Int) {
        if (this.maxWidth != maxWidth) {
            this.maxWidth = maxWidth
            requestLayout()
        }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val childCount = childCount
        if (childCount < 2) {
            throw RuntimeException("CustomLayout child count must >= 2")
        }
        if (getChildAt(0) !is TextView) {
            throw RuntimeException("CustomLayout first child view not a TextView")
        }
        val paddingWidth = paddingStart + paddingEnd
        val paddingHeight = paddingTop + paddingBottom

        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            measureChildren(
                widthMeasureSpec,
                heightMeasureSpec
            )
        } else {
            measureChildren(
                MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
                heightMeasureSpec
            )
        }

        val firstView = getChildAt(0) as TextView
        val secondView = getChildAt(1)
        val third = getThird()
        initTextParams(firstView)

        val layoutHeight: Int
        val layoutWidth: Int

        if (isRtl) {
            layoutWidth = firstView.measuredWidth + contentPadding * 2
            layoutHeight = firstView.measuredHeight + secondView.measuredHeight + contentPadding * 2
        } else if (lastLineWidth + offset + secondView.measuredWidth <= firstView.measuredWidth) {
            layoutWidth = firstView.measuredWidth + contentPadding * 2
            layoutHeight = firstView.measuredHeight + contentPadding * 2
        } else if (secondView.measuredWidth > firstView.measuredWidth + contentPadding * 2) {
            if (secondView.measuredWidth + offset + firstView.measuredWidth < maxWidth - paddingWidth) {
                layoutWidth =
                    firstView.measuredWidth + offset + secondView.measuredWidth + contentPadding * 2
                layoutHeight = firstView.measuredHeight + contentPadding * 2
            } else {
                layoutWidth = secondView.measuredWidth
                layoutHeight =
                    firstView.measuredHeight + secondView.measuredHeight + contentPadding * 2
            }
        } else if (lastLineWidth == firstView.measuredWidth.toFloat() && lastLineWidth + offset + secondView.measuredWidth < maxWidth - paddingWidth) {
            layoutWidth =
                (lastLineWidth + offset + secondView.measuredWidth).toInt() + contentPadding * 2
            layoutHeight = firstView.measuredHeight + contentPadding * 2
        } else if (third != null && lastLineWidth + offset + secondView.measuredWidth <= third.measuredWidth) {
            layoutWidth = third.measuredWidth + contentPadding * 2
            layoutHeight = firstView.measuredHeight + contentPadding * 2
        } else {
            layoutWidth = firstView.measuredWidth + contentPadding * 2
            layoutHeight = firstView.measuredHeight + secondView.measuredHeight + contentPadding * 2
        }
        if (third != null) {
            val lp = third.layoutParams as MarginLayoutParams
            setMeasuredDimension(
                max(layoutWidth, third.measuredWidth) + paddingWidth,
                layoutHeight + third.measuredHeight + paddingHeight + lp.topMargin + lp.bottomMargin,
            )
            if (third is ViewGroup && third.measuredWidth < measuredWidth) {
                third.measure(
                    MeasureSpec.makeMeasureSpec(measuredWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(third.measuredHeight, MeasureSpec.EXACTLY),
                )
            }
        } else {
            setMeasuredDimension(layoutWidth + paddingWidth, layoutHeight + paddingHeight)
        }
    }

    private fun getThird(): View? {
        return if (childCount > 2) {
            val view = getChildAt(2)
            if (view.visibility == View.VISIBLE) {
                view
            } else {
                null
            }
        } else {
            null
        }
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        val firstView = getChildAt(0) as TextView
        val secondView = getChildAt(1)
        val thirdView = getThird()
        if (thirdView == null) {
            firstView.layout(
                paddingStart,
                paddingTop,
                firstView.measuredWidth + paddingStart,
                firstView.measuredHeight + paddingTop,
            )
        } else {
            val lp = thirdView.layoutParams as MarginLayoutParams
            thirdView.layout(
                paddingStart,
                paddingTop + lp.topMargin,
                thirdView.measuredWidth + paddingStart,
                thirdView.measuredHeight + paddingTop + lp.bottomMargin,
            )
            firstView.layout(
                paddingStart + contentPadding,
                paddingTop + thirdView.measuredHeight + lp.topMargin + lp.bottomMargin + contentPadding,
                firstView.measuredWidth + paddingStart + contentPadding,
                firstView.measuredHeight + paddingTop + thirdView.measuredHeight + lp.topMargin + lp.bottomMargin + contentPadding,
            )
        }
        val top = measuredHeight - paddingBottom - secondView.measuredHeight - contentPadding
        val left = measuredWidth - paddingEnd - secondView.measuredWidth - contentPadding
        secondView.layout(
            left,
            top,
            left + secondView.measuredWidth,
            top + secondView.measuredHeight,
        )
    }

    private fun initTextParams(textView: TextView) {
        val layout = textView.layout
        val lastLineIndex = textView.lineCount - 1
        val lastLineRight = layout.getLineRight(lastLineIndex)
        val lastLineLeft = layout.getLineLeft(lastLineIndex)
        lastLineWidth = lastLineRight - lastLineLeft
        isRtl = lastLineLeft > offset
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
