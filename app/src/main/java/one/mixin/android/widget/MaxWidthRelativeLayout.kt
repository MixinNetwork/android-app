package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import one.mixin.android.R
import one.mixin.android.extension.dp
class MaxWidthRelativeLayout : RelativeLayout {
    private var maxWidth: Int = 0
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.MaxWidthRelativeLayout, defStyleAttr, 0)
        maxWidth = ta.getDimensionPixelSize(R.styleable.MaxWidthRelativeLayout_mr_max_width, 300.dp)
        ta.recycle()
    }
    fun setMaxWidth(maxWidth: Int) {
        if (this.maxWidth != maxWidth) {
            this.maxWidth = maxWidth
            requestLayout()
        }
    }
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val measuredWidth = MeasureSpec.getSize(widthMeasureSpec)
        var wms = widthMeasureSpec
        if (maxWidth in 1 until measuredWidth) {
            val measureMode = MeasureSpec.getMode(widthMeasureSpec)
            wms = MeasureSpec.makeMeasureSpec(maxWidth, measureMode)
        }
        super.onMeasure(wms, heightMeasureSpec)
    }
}
