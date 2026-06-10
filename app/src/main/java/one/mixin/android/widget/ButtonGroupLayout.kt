package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup

class ButtonGroupLayout(context: Context, attrs: AttributeSet?) : ViewGroup(context, attrs) {

    private var horizontalSpacing = 0
    private var verticalSpacing = 0

    fun setLineSpacing(lineSpacing: Int) {
        horizontalSpacing = lineSpacing
        verticalSpacing = lineSpacing
    }

    private val layoutGird = mutableListOf<Int>()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        var totalHeight = 0
        layoutGird.clear()

        val miniWidth = (widthSize - paddingStart - paddingEnd - horizontalSpacing * 2) / 3
        val middleWidth = (widthSize - paddingStart - paddingEnd - horizontalSpacing) / 2
        val maxWidth = widthSize - paddingStart - paddingEnd

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            measureChild(child, MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST), heightMeasureSpec)
        }

        var i = 0
        while (i < childCount) {
            if (i + 2 < childCount) {
                val firstView = getChildAt(i)
                val secondView = getChildAt(i + 1)
                val thirdView = getChildAt(i + 2)
                if (firstView.measuredWidth + secondView.measuredWidth + thirdView.measuredWidth + horizontalSpacing * 2 <= maxWidth && firstView.measuredWidth<= miniWidth && secondView.measuredWidth <= miniWidth && thirdView.measuredWidth <= miniWidth) {
                    totalHeight += listOf(firstView.measuredHeight, secondView.measuredHeight, thirdView.measuredHeight).max() + verticalSpacing
                    layoutGird.add(1)
                    layoutGird.add(1)
                    layoutGird.add(1)
                    firstView.measure(MeasureSpec.makeMeasureSpec(miniWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
                    secondView.measure(MeasureSpec.makeMeasureSpec(miniWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
                    thirdView.measure(MeasureSpec.makeMeasureSpec(miniWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
                    i += 3
                } else if (firstView.measuredWidth + secondView.measuredWidth + horizontalSpacing <= maxWidth && firstView.measuredWidth<= middleWidth && secondView.measuredWidth <= middleWidth) {
                    totalHeight += listOf(firstView.measuredHeight, secondView.measuredHeight).max() + verticalSpacing
                    firstView.measure(MeasureSpec.makeMeasureSpec(middleWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
                    secondView.measure(MeasureSpec.makeMeasureSpec(middleWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
                    layoutGird.add(2)
                    layoutGird.add(2)
                    i += 2
                } else {
                    totalHeight += firstView.measuredHeight + verticalSpacing
                    firstView.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
                    layoutGird.add(3)
                    i += 1
                }
            } else if (i + 1 < childCount) {
                val firstView = getChildAt(i)
                val secondView = getChildAt(i + 1)
                if (firstView.measuredWidth + secondView.measuredWidth + horizontalSpacing <= maxWidth &&  firstView.measuredWidth<= middleWidth && secondView.measuredWidth <= middleWidth) {
                    totalHeight += listOf(firstView.measuredHeight, secondView.measuredHeight).max() + verticalSpacing
                    firstView.measure(MeasureSpec.makeMeasureSpec(middleWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
                    secondView.measure(MeasureSpec.makeMeasureSpec(middleWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
                    layoutGird.add(2)
                    layoutGird.add(2)
                    i += 2
                } else {
                    totalHeight += firstView.measuredHeight + verticalSpacing
                    firstView.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
                    layoutGird.add(3)
                    i += 1
                }
            } else if (i < childCount) {
                val firstView = getChildAt(i)
                totalHeight += firstView.measuredHeight + verticalSpacing
                firstView.measure(MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
                layoutGird.add(3)
                i += 1
            } else {
                break
            }
        }
        totalHeight -= verticalSpacing

        setMeasuredDimension(widthSize, totalHeight + paddingTop + paddingBottom)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val miniWidth = (measuredWidth - paddingStart - paddingEnd - horizontalSpacing * 2) / 3
        val middleWidth = (measuredWidth - paddingStart - paddingEnd - horizontalSpacing) / 2
        val maxWidth = measuredWidth - paddingStart - paddingEnd
        var curTop = paddingTop
        var curLeft = paddingStart
        var i = 0
        while (i < childCount)
            if (layoutGird[i] == 1) {
                val firstView = getChildAt(i)
                val secondView = getChildAt(i + 1)
                val thirdView = getChildAt(i + 2)
                firstView.layout(curLeft, curTop, curLeft + miniWidth, curTop + firstView.measuredHeight)
                curLeft += miniWidth + horizontalSpacing
                secondView.layout(curLeft, curTop, curLeft + miniWidth, curTop + secondView.measuredHeight)
                curLeft += miniWidth + horizontalSpacing
                thirdView.layout(curLeft, curTop, curLeft + miniWidth, curTop + thirdView.measuredHeight)
                curLeft = paddingStart
                curTop += listOf(firstView.measuredHeight, secondView.measuredHeight, thirdView.measuredHeight).max() + verticalSpacing
                i += 3
            } else if (layoutGird[i] == 2) {
                val firstView = getChildAt(i)
                val secondView = getChildAt(i + 1)
                firstView.layout(curLeft, curTop, curLeft + middleWidth, curTop + firstView.measuredHeight)
                curLeft += middleWidth + horizontalSpacing
                secondView.layout(curLeft, curTop, curLeft + middleWidth, curTop + secondView.measuredHeight)
                curLeft = paddingStart
                curTop += listOf(firstView.measuredHeight, secondView.measuredHeight).max() + verticalSpacing
                i += 2
            } else {
                val firstView = getChildAt(i)
                firstView.layout(curLeft, curTop, curLeft + maxWidth, curTop + firstView.measuredHeight)
                curLeft = paddingStart
                curTop += firstView.measuredHeight + verticalSpacing
                i += 1
            }
    }
}
