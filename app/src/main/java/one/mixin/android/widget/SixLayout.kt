package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

class SixLayout : ViewGroup {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val count = childCount
        val middleWidthPadding = (paddingStart + paddingEnd) / 2
        val middleHeightPadding = (paddingTop + paddingBottom) / 2
        val itemWidth = (measuredWidth - 3 * middleWidthPadding) / 2
        val itemHeight = (measuredHeight - 4 * middleHeightPadding) / 3
        for (i in 1..count) {
            val child = getChildAt(i - 1)
            val start = if (i % 2 == 0) {
                paddingStart + itemWidth + middleWidthPadding
            } else {
                paddingStart
            }
            val top = when {
                ((i - 1) / 2) == 0 -> {
                    paddingTop
                }
                ((i - 1) / 2) == 1 -> {
                    paddingTop + itemHeight + middleHeightPadding
                }
                else -> {
                    paddingTop + (itemHeight + middleHeightPadding) * 2
                }
            }
            child.layout(start, top, start + itemWidth, top + itemHeight)
        }
    }
}