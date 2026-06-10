package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.GridView

class FixedGridView(context: Context, attributeSet: AttributeSet) : GridView(context, attributeSet) {
    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val heightSpec =
            if (layoutParams.height == LayoutParams.WRAP_CONTENT) {
                MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE shr 2, MeasureSpec.AT_MOST)
            } else {
                heightMeasureSpec
            }
        super.onMeasure(widthMeasureSpec, heightSpec)
    }
}
