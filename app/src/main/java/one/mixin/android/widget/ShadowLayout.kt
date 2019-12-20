package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.core.view.marginTop

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
        when {
            childCount == 3 -> {
                val thirdView = getChildAt(2)
                firstView.layout(
                    firstLp.marginStart, thirdView.measuredHeight + firstLp.topMargin,
                    width - firstLp.marginEnd, firstView.measuredHeight
                )
                secondView.layout(
                    width - secondView.measuredWidth - secondView.marginEnd - 3,
                    height - secondView.measuredHeight - 6,
                    width - secondView.marginEnd - 3,
                    height - 6
                )
                thirdView.layout(
                    thirdView.marginStart,
                    thirdView.marginTop,
                    thirdView.marginStart + thirdView.measuredWidth,
                    thirdView.marginTop + thirdView.measuredHeight
                )
            }
            firstLp.marginStart > 0 -> {
                firstView.layout(
                    firstLp.marginStart, 0,
                    firstLp.marginStart + firstView.measuredWidth, firstView.measuredHeight
                )
                secondView.layout(
                    width - secondView.measuredWidth,
                    height - secondView.measuredHeight, width, height
                )
            }
            firstLp.marginEnd > 0 -> {
                firstView.layout(
                    width - firstLp.marginEnd - firstView.measuredWidth,
                    0, width - firstLp.marginEnd, firstView.measuredHeight
                )
                secondView.layout(
                    width - firstLp.marginEnd - secondView.measuredWidth,
                    height - secondView.measuredHeight, width - firstLp.marginEnd, height
                )
            }
            else -> {
                firstView.layout(0, 0, width, height)
                secondView.layout(
                    width - secondView.measuredWidth,
                    height - secondView.measuredHeight, width, height
                )
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}
