package one.mixin.android.widget

import android.content.Context
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import one.mixin.android.extension.dpToPx

class MessageLayout : ViewGroup {
    private val offset: Int
    private var type: Int = 0
    private var lastLineTop: Int = 0
    private var lastLineRight: Float = 0.toFloat()

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        offset = context.dpToPx(8f)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childCount = childCount
        if (childCount < 2) {
            throw RuntimeException("CustomLayout child count must >= 2")
        }
        if (getChildAt(0) !is TextView) {
            throw RuntimeException("CustomLayout first child view not a TextView")
        }
        val paddingWidth = paddingStart + paddingEnd
        val paddingHeight = paddingTop + paddingBottom
        val w = View.MeasureSpec.getSize(widthMeasureSpec)

        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val firstView = getChildAt(0) as TextView
        val secondView = getChildAt(1)
        val third = getThird()
        initTextParams(firstView.text, firstView.measuredWidth, firstView.paint)

        type = when {
            firstView.measuredWidth + secondView.measuredWidth + offset <= w - paddingWidth -> {
                val width = firstView.measuredWidth + secondView.measuredWidth
                val height = Math.max(firstView.measuredHeight, secondView.measuredHeight)
                if (third != null) {
                    if (third.measuredWidth > width + offset) {
                        setMeasuredDimension(third.measuredWidth + paddingWidth,
                            height + paddingHeight + third.measuredHeight)
                    } else {
                        setMeasuredDimension(width + paddingWidth + offset,
                            height + paddingHeight + third.measuredHeight)
                    }
                } else {
                    setMeasuredDimension(width + paddingWidth + offset,
                        height + paddingHeight)
                }
                SINGLE_LINE
            }
            lastLineRight + secondView.measuredWidth + offset > w - paddingWidth -> {
                if (third != null) {
                    setMeasuredDimension(firstView.measuredWidth + paddingWidth,
                        firstView.measuredHeight + secondView.measuredHeight + third.measuredHeight + paddingHeight)
                } else {
                    setMeasuredDimension(firstView.measuredWidth + paddingWidth,
                        firstView.measuredHeight + secondView.measuredHeight + paddingHeight)
                }
                NEXT_LINE
            }
            lastLineRight + secondView.measuredWidth + offset > firstView.measuredWidth -> {
                val height = Math.max(firstView.measuredHeight, lastLineTop + secondView.measuredHeight)
                if (third != null) {
                    setMeasuredDimension(lastLineRight.toInt() + secondView.measuredWidth + offset + paddingWidth,
                        height + third.measuredHeight + paddingHeight)
                } else {
                    setMeasuredDimension(lastLineRight.toInt() + secondView.measuredWidth + offset + paddingWidth,
                        height + paddingHeight)
                }
                MULTI_LINE_SMALL
            }
            else -> {
                val height = Math.max(firstView.measuredHeight, lastLineTop + secondView.measuredHeight)
                if (third != null) {
                    if (third.measuredWidth > firstView.measuredWidth) {
                        setMeasuredDimension(third.measuredWidth + paddingWidth,
                            height + third.measuredHeight + paddingHeight)
                    } else {
                        setMeasuredDimension(firstView.measuredWidth + paddingWidth,
                            height + third.measuredHeight + paddingHeight)
                    }
                } else {
                    setMeasuredDimension(firstView.measuredWidth + paddingWidth,
                        height + paddingHeight)
                }
                MULTI_LINE
            }
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

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val firstView = getChildAt(0) as TextView
        val secondView = getChildAt(1)
        val thirdView = getThird()
        if (thirdView == null) {
            firstView.layout(paddingStart, paddingTop, firstView.measuredWidth + paddingStart,
                firstView.measuredHeight + paddingTop)
        } else {
            thirdView.layout(paddingStart, paddingTop, thirdView.measuredWidth + paddingStart,
                thirdView.measuredHeight + paddingTop)
            firstView.layout(paddingStart, paddingTop + thirdView.measuredHeight,
                firstView.measuredWidth + paddingStart,
                firstView.measuredHeight + paddingTop + thirdView.measuredHeight)
        }
        when (type) {
            SINGLE_LINE -> {
                val left = if (thirdView == null) {
                    (lastLineRight + paddingStart).toInt() + offset
                } else {
                    r - l - paddingEnd - secondView.measuredWidth
                }
                val top = if (thirdView == null) {
                    paddingTop + firstView.measuredHeight - secondView.measuredHeight
                } else {
                    paddingTop + firstView.measuredHeight - secondView.measuredHeight + thirdView.measuredHeight
                }
                secondView.layout(left, top, left + secondView.measuredWidth, top + secondView.measuredHeight)
            }
            NEXT_LINE -> {
                val left = firstView.measuredWidth + paddingStart - secondView.measuredWidth
                val top = if (thirdView == null) {
                    paddingTop + firstView.measuredHeight
                } else {
                    paddingTop + firstView.measuredHeight + thirdView.measuredHeight
                }
                secondView.layout(left, top, left + secondView.measuredWidth, top + secondView.measuredHeight)
            }
            MULTI_LINE_SMALL -> {
                val left = (lastLineRight + paddingStart).toInt() + offset
                val top = if (thirdView == null) {
                    firstView.measuredHeight + paddingTop - secondView.measuredHeight
                } else {
                    firstView.measuredHeight + paddingTop - secondView.measuredHeight + thirdView.measuredHeight
                }
                secondView.layout(left, top, left + secondView.measuredWidth, top + secondView.measuredHeight)
            }
            MULTI_LINE -> {
                val left = width - paddingEnd - secondView.measuredWidth
                val top = if (thirdView == null) {
                    firstView.measuredHeight + paddingTop - secondView.measuredHeight
                } else {
                    firstView.measuredHeight + paddingTop - secondView.measuredHeight + thirdView.measuredHeight
                }
                secondView.layout(left, top, left + secondView.measuredWidth, top + secondView.measuredHeight)
            }
        }
    }

    private fun initTextParams(text: CharSequence, maxWidth: Int, paint: TextPaint) {
        val staticLayout = StaticLayout(text, paint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false)
        val lineCount = staticLayout.lineCount
        lastLineTop = staticLayout.getLineTop(lineCount - 1)
        lastLineRight = staticLayout.getLineRight(lineCount - 1)
    }

    companion object {
        private val SINGLE_LINE = 0x01
        private val MULTI_LINE = 0x02
        private val MULTI_LINE_SMALL = 0x03
        private val NEXT_LINE = 0x04
    }
}