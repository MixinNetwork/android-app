package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.RelativeLayout

class PaddingRelativeLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr) {

    private val minPaddingInPx: Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 10f, resources.displayMetrics
    ).toInt()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = measuredWidth

        val calculatedPadding = (width * 0.05).toInt()
        val finalPadding = maxOf(calculatedPadding, minPaddingInPx)

        setPadding(finalPadding, paddingTop, finalPadding, paddingBottom)
    }
}