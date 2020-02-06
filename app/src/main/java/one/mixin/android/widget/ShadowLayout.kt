package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

class ShadowLayout : ViewGroup {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val firstView = getChildAt(0)
        val firstLp = firstView.layoutParams as MarginLayoutParams
        setMeasuredDimension(
            firstView.measuredWidth + firstLp.marginStart + firstLp.marginEnd,
            firstView.measuredHeight + firstLp.topMargin + firstLp.bottomMargin
        )
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val firstView = getChildAt(0)
        val firstLp = firstView.layoutParams as MarginLayoutParams
        val secondView = getChildAt(1)
        val secondLp = secondView.layoutParams as MarginLayoutParams
        when {
            childCount == 3 -> {
                val thirdView = getChildAt(2)
                val thirdLp = thirdView.layoutParams as MarginLayoutParams
                firstView.layout(
                    firstLp.marginStart, firstLp.topMargin,
                    width - firstLp.marginEnd, height - firstLp.bottomMargin
                )
                secondView.layout(
                    width - secondView.measuredWidth - secondLp.marginEnd,
                    height - secondView.measuredHeight - secondLp.bottomMargin,
                    width - secondLp.marginEnd,
                    height - secondLp.bottomMargin
                )
                thirdView.layout(
                    width - thirdView.measuredWidth - thirdLp.marginEnd,
                    thirdLp.topMargin,
                    width - thirdLp.marginEnd,
                    thirdLp.topMargin + thirdView.measuredHeight
                )
            }
            firstLp.marginStart > 0 -> {
                firstView.layout(
                    firstLp.marginStart, 0,
                    firstLp.marginStart + firstView.measuredWidth, firstView.measuredHeight
                )
                secondView.layout(
                    width - secondView.measuredWidth - secondLp.marginEnd,
                    height - secondView.measuredHeight - secondLp.bottomMargin,
                    width - secondLp.marginEnd,
                    height - secondLp.bottomMargin
                )
            }
            firstLp.marginEnd > 0 -> {
                firstView.layout(
                    width - firstLp.marginEnd - firstView.measuredWidth,
                    0, width - firstLp.marginEnd, firstView.measuredHeight
                )
                secondView.layout(
                    width - secondView.measuredWidth - secondLp.marginEnd,
                    height - secondView.measuredHeight - secondLp.bottomMargin,
                    width - secondLp.marginEnd,
                    height - secondLp.bottomMargin
                )
            }
            else -> {
                firstView.layout(0, 0, width, height)
                secondView.layout(
                    width - secondView.measuredWidth - secondLp.marginEnd,
                    height - secondView.measuredHeight - secondLp.bottomMargin,
                    width - secondLp.marginEnd,
                    height - secondLp.bottomMargin
                )
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}
