package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

class ShadowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val firstView = getChildAt(0)
        val firstLp = firstView.layoutParams as ViewGroup.MarginLayoutParams
        setMeasuredDimension(firstView.measuredWidth + firstLp.marginStart + firstLp.marginEnd,
            firstView.measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val firstView = getChildAt(0)
        val firstLp = firstView.layoutParams as ViewGroup.MarginLayoutParams
        val secondView = getChildAt(1)
        when {
            firstLp.marginStart > 0 -> {
                firstView.layout(firstLp.marginStart, 0,
                    firstLp.marginStart + firstView.measuredWidth, firstView.measuredHeight)
                secondView.layout(width - secondView.measuredWidth,
                    height - secondView.measuredHeight, width, height)
            }
            firstLp.marginEnd > 0 -> {
                firstView.layout(width - firstLp.marginEnd - firstView.measuredWidth,
                    0, width - firstLp.marginEnd, firstView.measuredHeight)
                secondView.layout(width - firstLp.marginEnd - secondView.measuredWidth,
                    height - secondView.measuredHeight, width - firstLp.marginEnd, height)
            }
            else -> {
                firstView.layout(0, 0, width, height)
                secondView.layout(width - secondView.measuredWidth,
                    height - secondView.measuredHeight, width, height)
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): ViewGroup.LayoutParams {
        return ViewGroup.MarginLayoutParams(context, attrs)
    }
}
