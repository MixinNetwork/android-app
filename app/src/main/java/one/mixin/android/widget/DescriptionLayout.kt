package one.mixin.android.widget

import android.annotation.SuppressLint
import android.content.Context
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import one.mixin.android.R

class DescriptionLayout : ViewGroup {
    private var type: Int = 0
    private var lineCount: Int = 0
    private var lineHeight: Int = 0
    private var lineWidth: Int = 0
    private var isExpand: Boolean = false

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun expand() {
        isExpand = true
        requestLayout()
    }

    @SuppressLint("SetTextI18n")
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childCount = childCount
        if (childCount != 2) {
            throw RuntimeException("CustomLayout child count must == 2")
        }
        if (getChildAt(0) !is TextView) {
            throw RuntimeException("CustomLayout first child view not a TextView")
        }
        val paddingWidth = paddingStart + paddingEnd
        val paddingHeight = paddingTop + paddingBottom

        val w = View.MeasureSpec.getSize(widthMeasureSpec)

        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val firstView = getChildAt(0) as TextView
        val secondView = getChildAt(1) as TextView
        initTextParams(firstView.text, firstView.measuredWidth, firstView.paint)

        type = when {
            isExpand -> {
                val height = firstView.measuredHeight
                setMeasuredDimension(w + paddingWidth, height + paddingHeight)
                EXPAND
            }
            lineCount <= 3 -> {
                val height = firstView.measuredHeight
                setMeasuredDimension(w + paddingWidth, height + paddingHeight)
                DEFAULT
            }
            lineWidth + secondView.measuredWidth <= w - paddingWidth -> {
                val height = lineHeight * 3
                setMeasuredDimension(w + paddingWidth, height + paddingHeight)
                secondView.text = " ${context.getString(R.string.show_more)}"
                TAIL
            }
            else -> {
                val height = lineHeight * 3
                setMeasuredDimension(w + paddingWidth, height + paddingHeight)
                secondView.setText(R.string.show_more)
                BOTTOM
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val firstView = getChildAt(0) as TextView
        val secondView = getChildAt(1)
        firstView.layout(
            paddingStart,
            paddingTop,
            firstView.measuredWidth + paddingStart,
            firstView.measuredHeight + paddingTop
        )
        when (type) {
            EXPAND, DEFAULT -> {
                secondView.layout(0, 0, 0, 0)
                firstView.layout(
                    paddingStart,
                    paddingTop,
                    firstView.measuredWidth + paddingStart,
                    firstView
                        .measuredHeight + paddingTop
                )
            }
            TAIL -> {
                val left = paddingStart + lineWidth
                val top = lineHeight * 3 + paddingTop - lineHeight
                firstView.layout(
                    paddingStart,
                    paddingTop,
                    firstView.measuredWidth + paddingStart,
                    firstView
                        .measuredHeight + paddingTop
                )
                secondView.layout(left, top, left + secondView.measuredWidth, top + lineHeight)
            }
            BOTTOM -> {
                val top = lineHeight * 3 + paddingTop - lineHeight
                firstView.layout(
                    paddingStart,
                    paddingTop,
                    firstView.measuredWidth + paddingStart,
                    paddingTop + lineHeight * 2
                )
                secondView.layout(paddingStart, top, firstView.measuredWidth + paddingStart, top + lineHeight)
            }
        }
    }

    private fun initTextParams(text: CharSequence, maxWidth: Int, paint: TextPaint) {
        val string = text.trim()
        val staticLayout = StaticLayout.Builder.obtain(string, 0, string.length, paint, maxWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false)
            .setLineSpacing(0.0f, 1.0f).build()

        lineCount = staticLayout.lineCount
        val lastLine = if (lineCount > 3) {
            2
        } else {
            lineCount - 1
        }
        lineHeight = staticLayout.getLineBottom(lastLine) - staticLayout.getLineTop(lastLine)
        lineWidth = (staticLayout.getLineRight(lastLine) - staticLayout.getLineLeft(lastLine)).toInt()
    }

    companion object {
        private const val DEFAULT = 0x01
        private const val TAIL = 0x02
        private const val BOTTOM = 0x03
        private const val EXPAND = 0x00
    }
}
