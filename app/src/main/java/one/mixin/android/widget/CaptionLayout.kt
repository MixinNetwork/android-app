package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.isVisible
import one.mixin.android.extension.dp
import one.mixin.android.extension.realSize
import timber.log.Timber

class CaptionLayout : ViewGroup {
    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val minWidth by lazy {
        (context.realSize().x * 0.5).toInt()
    }

    private val offset by lazy {
        4.dp
    }

    private var ratio = 0f

    fun setRatio(ratio: Float) {
        if (this.ratio != ratio) {
            this.ratio = ratio
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val childCount = childCount
        if (childCount != 3) {
            throw RuntimeException("CaptionLayout child count must == 3")
        }
        val firstView = getChildAt(0)
        val secondView = getChildAt(1)
        val thirdView = getChildAt(2)

        if (ratio != 0f) {
            measureChild(
                secondView,
                MeasureSpec.makeMeasureSpec(minWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((minWidth / ratio).toInt(), MeasureSpec.EXACTLY)
            )
        } else {
            measureChild(
                secondView,
                widthMeasureSpec,
                heightMeasureSpec
            )
        }

        measureChild(
            firstView,
            MeasureSpec.makeMeasureSpec(
                secondView.measuredWidth,
                MeasureSpec.EXACTLY
            ),
            heightMeasureSpec
        )

        measureChild(
            thirdView,
            MeasureSpec.makeMeasureSpec(
                secondView.measuredWidth - offset * 2,
                MeasureSpec.EXACTLY
            ),
            heightMeasureSpec
        )

        if (firstView.isVisible) {
            setMeasuredDimension(
                secondView.measuredWidth,
                firstView.measuredHeight + secondView.measuredHeight + thirdView.measuredHeight + offset * 2
            )
        } else {
            setMeasuredDimension(
                secondView.measuredWidth,
                secondView.measuredHeight + thirdView.measuredHeight + offset * 2
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val firstView = getChildAt(0)
        val secondView = getChildAt(1)
        val thirdView = getChildAt(2)
        var top = 0
        if (firstView.isVisible) {
            firstView.layout(0, top, measuredWidth, firstView.measuredHeight + top)
            top = firstView.measuredHeight
        }
        secondView.layout(
            0,
            top,
            measuredWidth,
            top + secondView.measuredHeight
        )
        top += secondView.measuredHeight + offset
        Timber.e(
            "$offset $top ${thirdView.measuredHeight} ${b + thirdView.measuredHeight}"
        )
        thirdView.layout(
            offset,
            top,
             offset + thirdView.measuredWidth,
            top + thirdView.measuredHeight
        )
    }

    override fun generateDefaultLayoutParams(): MarginLayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    override fun generateLayoutParams(p: LayoutParams): MarginLayoutParams {
        return MarginLayoutParams(p)
    }

    override fun generateLayoutParams(attrs: AttributeSet): MarginLayoutParams {
        return MarginLayoutParams(context, attrs)
    }
}