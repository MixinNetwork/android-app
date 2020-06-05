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
        if (childCount < 2) {
            throw RuntimeException("QuoteLayout child count must >=2")
        }
        val firstView = getChildAt(0)
        val secondView = getChildAt(1)
        if (ratio != 0f) {
            measureChild(
                secondView, MeasureSpec.makeMeasureSpec(minWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((minWidth / ratio).toInt(), MeasureSpec.EXACTLY)
            )
        } else {
            measureChild(
                secondView, widthMeasureSpec,
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
        if (childCount >= 3) {
            val thirdView = getChildAt(2)

            measureChild(
                thirdView,
                MeasureSpec.makeMeasureSpec(minWidth, MeasureSpec.AT_MOST),
                heightMeasureSpec
            )
        }

        setMeasuredDimension(
            secondView.measuredWidth + offset * 2,
            firstView.measuredHeight + secondView.measuredHeight + offset * 3
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val firstView = getChildAt(0)
        val secondView = getChildAt(1)
        firstView.layout(offset, offset, width - offset, firstView.measuredHeight + offset)
        secondView.layout(
            offset,
            height - secondView.measuredHeight - offset,
            width - offset,
            height - offset
        )
        if (childCount >= 3) {
            val thirdView = getChildAt(2)
            val lp = thirdView.layoutParams as MarginLayoutParams
            thirdView.layout(
                width - thirdView.measuredWidth - offset - lp.marginEnd,
                height - thirdView.measuredHeight - offset - lp.bottomMargin,
                width - offset - lp.marginEnd,
                height - offset - lp.bottomMargin
            )
        }
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
