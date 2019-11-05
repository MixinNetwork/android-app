package one.mixin.android.ui.common.biometric

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.children
import one.mixin.android.extension.statusBarHeight
import org.jetbrains.anko.collections.forEachReversedByIndex

class ScrollableBottomSheetLayout(context: Context, attributeSet: AttributeSet) : ViewGroup(context, attributeSet) {

    private val statusBarHeight = context.statusBarHeight()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var heightSpec = 0
        children.forEach { c ->
            measureChild(c, widthMeasureSpec, heightMeasureSpec)
            heightSpec += c.measuredHeight
        }
        val diffHeight = heightSpec - MeasureSpec.getSize(heightMeasureSpec)
        if (diffHeight > 0) {
            val scrollView = getChildAt(1)
            measureChild(scrollView, widthMeasureSpec,
                MeasureSpec.makeMeasureSpec(scrollView.measuredHeight - diffHeight, MeasureSpec.EXACTLY))
        } else {
            setMeasuredDimension(widthMeasureSpec, MeasureSpec.makeMeasureSpec(heightSpec, MeasureSpec.EXACTLY))
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var bottom = statusBarHeight
        var top = 0
        children.toList().forEachReversedByIndex { c ->
            bottom += c.measuredHeight
            c.layout(l, b - bottom, r, b - top)
            top += c.measuredHeight
        }
    }
}
