package one.mixin.android.ui.common.biometric

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isGone
import one.mixin.android.extension.statusBarHeight

class ScrollableBottomSheetLayout(context: Context, attributeSet: AttributeSet) : ViewGroup(context, attributeSet) {

    private val statusBarHeight = context.statusBarHeight()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentHeightSpec = MeasureSpec.makeMeasureSpec(
            MeasureSpec.getSize(heightMeasureSpec) - statusBarHeight,
            MeasureSpec.getMode(heightMeasureSpec)
        )
        super.onMeasure(widthMeasureSpec, parentHeightSpec)
        var heightSpec = 0
        children.forEach { c ->
            measureChild(c, widthMeasureSpec, parentHeightSpec)
            heightSpec += c.measuredHeight
        }
        val diffHeight = heightSpec - MeasureSpec.getSize(parentHeightSpec)
        if (diffHeight > 0) {
            val scrollView = getChildAt(1)
            measureChild(
                scrollView,
                widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(scrollView.measuredHeight - diffHeight, MeasureSpec.EXACTLY)
            )
        } else {
            setMeasuredDimension(widthMeasureSpec, MeasureSpec.makeMeasureSpec(heightSpec, MeasureSpec.EXACTLY))
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var bottom = 0
        var top = 0
        forEachReversedChild { c ->
            if (c.isGone) return@forEachReversedChild
            bottom += c.measuredHeight
            c.layout(l, b - bottom, r, b - top)
            top += c.measuredHeight
        }
    }

    private fun forEachReversedChild(f: (View) -> Unit) {
        var i = childCount - 1
        while (i >= 0) {
            val c = getChildAt(i)
            f(c)
            i--
        }
    }
}
