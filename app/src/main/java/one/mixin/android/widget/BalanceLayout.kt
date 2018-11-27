package one.mixin.android.widget

import android.content.Context
import android.text.Layout
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import one.mixin.android.extension.dpToPx

class BalanceLayout : ViewGroup {
    companion object {
        const val MIN_LINE_CHAR_COUNT = 3
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val symbolOffset by lazy { context.dpToPx(8f) }
    private var balanceWordWidth: Int? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val balanceTv = getChildAt(0) as TextView
        val symbolTv = getChildAt(1) as TextView
        val symbolMeasureWidth = symbolTv.measuredWidth + symbolTv.paddingStart + symbolTv.paddingEnd
        val balanceMeasureWidth = balanceTv.measuredWidth

        val lines = balanceTv.layout.lineCount
        val lastLineText = getLastLineText(balanceTv.layout, lines)
        if (balanceWordWidth == null) {
            // only for mono fonts
            balanceWordWidth = balanceTv.layout.paint.measureText(balanceTv.text[0].toString()).toInt()
        }
        val maxBalanceWidth = if (lines <= 1 && measuredWidth < balanceMeasureWidth + symbolMeasureWidth + symbolOffset) {
            // add MIN_LINE_CHAR_COUNT chars to a new line
            Math.min(measuredWidth - symbolMeasureWidth - symbolOffset, balanceTv.layout.width - balanceWordWidth!! * MIN_LINE_CHAR_COUNT)
        } else if (lines > 1) {
            if (lastLineText.length < MIN_LINE_CHAR_COUNT) {
                // if lines > 2, every line reduce 1 char, the last line over 2 chars.
                val reduceCount = if (lines > 2) 1 else 2
                balanceTv.layout.width - balanceWordWidth!! * reduceCount
            } else {
                val lastLineTextWidth = balanceTv.layout.paint.measureText(lastLineText.toString()).toInt()
                if (measuredWidth < lastLineTextWidth + symbolMeasureWidth + symbolOffset) {
                    // every line decrease one char's width is enough
                    balanceTv.layout.width - balanceWordWidth!!
                } else {
                    balanceTv.layout.width
                }
            }
        } else {
            balanceTv.layout.width
        }
        measureChild(balanceTv, MeasureSpec.makeMeasureSpec(maxBalanceWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(balanceTv.layout.height, MeasureSpec.UNSPECIFIED))
        measureChild(symbolTv, MeasureSpec.makeMeasureSpec(symbolMeasureWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(symbolTv.layout.height, MeasureSpec.EXACTLY))
        setMeasuredDimension(widthMeasureSpec, MeasureSpec.makeMeasureSpec(balanceTv.measuredHeight, MeasureSpec.EXACTLY))
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val balanceTv = getChildAt(0) as TextView
        val symbolTv = getChildAt(1) as TextView
        balanceTv.layout(0, 0, balanceTv.measuredWidth, balanceTv.measuredHeight)
        val symbolTop = balanceTv.height - symbolTv.measuredHeight
        val lines = balanceTv.layout.lineCount
        val balanceWidth = if (lines == 1) {
            balanceTv.measuredWidth
        } else {
            val lastLineText = getLastLineText(balanceTv.layout, lines)
            balanceTv.layout.paint.measureText(lastLineText.toString()).toInt()
        }
        val symbolLeft = balanceWidth + symbolOffset
        symbolTv.layout(symbolLeft, symbolTop, symbolLeft + symbolTv.measuredWidth +
            symbolTv.paddingStart + symbolTv.paddingEnd, symbolTop + symbolTv.measuredHeight)
    }

    private fun getLastLineText(balanceLayout: Layout, lines: Int): CharSequence {
        val lastLineStart = balanceLayout.getLineStart(lines - 1)
        val lastLineEnd = balanceLayout.getLineEnd(lines - 1)
        return balanceLayout.text.subSequence(lastLineStart, lastLineEnd)
    }
}