package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import one.mixin.android.extension.statusBarHeight

class BottomSheetConstraintLayout(context: Context, attributeSet: AttributeSet) : ConstraintLayout(context, attributeSet) {
    private val statusBarHeight = context.statusBarHeight()

    var heightOffset: Int = statusBarHeight
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val parentHeightSpec =
            MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(heightMeasureSpec) - heightOffset,
                MeasureSpec.getMode(heightMeasureSpec),
            )
        super.onMeasure(widthMeasureSpec, parentHeightSpec)
    }
}
