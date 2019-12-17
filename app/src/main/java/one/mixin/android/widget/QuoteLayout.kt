package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import one.mixin.android.extension.dpToPx
import one.mixin.android.extension.realSize

class QuoteLayout : ViewGroup {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val minWidth by lazy {
        (context.realSize().x * 0.5).toInt()
    }

    private val offset by lazy {
        context.dpToPx(1.5f)
    }

    private var ratio = 0f

    fun setRatio(ratio: Float) {
        if (this.ratio != ratio) {
            this.ratio = ratio
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childCount = childCount
        if (childCount != 3) {
            throw RuntimeException("CustomLayout child count must == 3")
        }
        val firstView = getChildAt(0)
        val secondView = getChildAt(1)
        val thirdView = getChildAt(2)
        if (ratio != 0f) {
            measureChild(
                secondView, MeasureSpec.makeMeasureSpec(minWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((minWidth / ratio).toInt(), MeasureSpec.EXACTLY)
            )
        } else {
            measureChild(
                secondView, MeasureSpec.makeMeasureSpec(minWidth, MeasureSpec.AT_MOST),
                heightMeasureSpec
            )
        }
        measureChild(
            firstView,
            MeasureSpec.makeMeasureSpec(
                secondView.measuredWidth,
                MeasureSpec.EXACTLY
            ),
            heightMeasureSpec
        )
        measureChild(
            thirdView,
            MeasureSpec.makeMeasureSpec(minWidth, MeasureSpec.AT_MOST),
            heightMeasureSpec
        )

        setMeasuredDimension(
            secondView.measuredWidth + offset * 2,
            firstView.measuredHeight + secondView.measuredHeight + offset * 3
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val firstView = getChildAt(0)
        val secondView = getChildAt(1)
        val thirdView = getChildAt(2)
        firstView.layout(offset, offset, width - offset, firstView.measuredHeight + offset)
        secondView.layout(
            offset,
            height - secondView.measuredHeight - offset,
            width - offset,
            height - offset
        )
        thirdView.layout(
            width - thirdView.measuredWidth - offset,
            height - thirdView.measuredHeight - offset,
            width - offset,
            height - offset
        )
    }
}
