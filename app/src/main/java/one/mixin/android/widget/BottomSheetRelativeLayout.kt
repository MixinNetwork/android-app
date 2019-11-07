package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import android.widget.RelativeLayout
import one.mixin.android.extension.statusBarHeight

class BottomSheetRelativeLayout(context: Context, attributeSet: AttributeSet) : RelativeLayout(context, attributeSet) {

    private val statusBarHeight = context.statusBarHeight()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val parentHeightSpec = MeasureSpec.makeMeasureSpec(
            MeasureSpec.getSize(heightMeasureSpec) - statusBarHeight,
            MeasureSpec.getMode(heightMeasureSpec)
        )
        super.onMeasure(widthMeasureSpec, parentHeightSpec)
    }
}
