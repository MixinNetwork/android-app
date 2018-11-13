package one.mixin.android.widget

import android.content.Context
import android.text.Layout
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.TextView
import one.mixin.android.extension.dpToPx

class BalanceLayout : ViewGroup {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val symbolOffset by lazy { context.dpToPx(8f) }
    private var wordWidth: Int? = null

    private var changedWidth: Int? = null
    private var changedHeight: Int? = null

    var listener: BalanceLayout.OnBalanceLayoutListener? = null

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val balanceTv = getChildAt(0) as TextView
        val symbolTv = getChildAt(1) as TextView
        val symbolMeasureWidth = symbolTv.measuredWidth + symbolTv.paddingStart + symbolTv.paddingEnd
        val balanceMeasureWidth = balanceTv.measuredWidth
        val preBalanceHeight = balanceTv.measuredHeight

        if (changedWidth != null && changedHeight != null) {
            measureChild(balanceTv, MeasureSpec.makeMeasureSpec(changedWidth!!, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(changedHeight!!, MeasureSpec.EXACTLY))
        }

        val balanceLayout = balanceTv.layout
        val lines = balanceLayout.lineCount
        val maxBalanceWidth = if ((lines <= 1 && measuredWidth < balanceMeasureWidth + symbolMeasureWidth + symbolOffset)
            || lines > 1) {
            val lastLineText = getLastLineText(balanceLayout, lines)
            if (lastLineText.length < 3) {
                // if lines > 2, every line reduce 1 word, the last line over 2 words.
                val reduceCount = if (lines > 2) 1 else 2
                val balancePaint = balanceTv.layout.paint
                // only for mono fonts
                wordWidth = balancePaint.measureText(lastLineText.get(0).toString()).toInt()
                balanceTv.layout.width - wordWidth!! * reduceCount
            } else {
                changedWidth = measuredWidth - symbolMeasureWidth - symbolOffset
                measureChild(balanceTv, MeasureSpec.makeMeasureSpec(changedWidth!!, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(balanceTv.layout.height, MeasureSpec.UNSPECIFIED))
                changedHeight = balanceTv.measuredHeight
                listener?.onBalanceHeightChange(changedHeight!! - preBalanceHeight)
                return
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
        val lastLineText = balanceLayout.text.subSequence(lastLineStart, lastLineEnd)
        return lastLineText
    }

    interface OnBalanceLayoutListener {
        fun onBalanceHeightChange(diffHeight: Int)
    }
}