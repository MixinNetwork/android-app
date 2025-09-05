package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import one.mixin.android.R
import androidx.core.view.isVisible

class TitleTailLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxHeight = 0
        var totalWidth = 0
        
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                maxHeight = maxOf(maxHeight, child.measuredHeight + getTopMargin(child) + getBottomMargin(child))
            }
        }
        
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        
        var textView: View? = null
        var tailIcon: View? = null
        var otherWidthUsed = 0
        
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                when {
                    child.id == R.id.title_tv -> textView = child
                    child.id == R.id.tail_icon -> tailIcon = child
                    else -> otherWidthUsed += child.measuredWidth + getLeftMargin(child) + getRightMargin(child)
                }
            }
        }
        
        var tailWidth = 0
        tailIcon?.let { tail ->
            if (tail.isVisible) {
                tailWidth = tail.measuredWidth + getLeftMargin(tail) + getRightMargin(tail)
            }
        }
        
        textView?.let { text ->
            val textAvailableWidth = availableWidth - otherWidthUsed - tailWidth
            if (textAvailableWidth > 0) {
                val textWidthSpec = MeasureSpec.makeMeasureSpec(textAvailableWidth, MeasureSpec.AT_MOST)
                text.measure(textWidthSpec, heightMeasureSpec)
                maxHeight = maxOf(maxHeight, text.measuredHeight + getTopMargin(text) + getBottomMargin(text))
            }
        }

        totalWidth = paddingLeft + paddingRight
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                totalWidth += child.measuredWidth + getLeftMargin(child) + getRightMargin(child)
            }
        }
        val totalHeight = paddingTop + paddingBottom + maxHeight
        
        setMeasuredDimension(totalWidth, totalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingBottom = paddingBottom
        
        var currentLeft = paddingLeft
        val layoutHeight = b - t - paddingTop - paddingBottom
        
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight
                val leftMargin = getLeftMargin(child)
                val topMargin = getTopMargin(child)
                val rightMargin = getRightMargin(child)
                
                val childTop = paddingTop + (layoutHeight - childHeight) / 2
                val childLeft = currentLeft + leftMargin
                
                child.layout(
                    childLeft,
                    childTop,
                    childLeft + childWidth,
                    childTop + childHeight
                )
                
                currentLeft = childLeft + childWidth + rightMargin
            }
        }
    }
    
    private fun getLeftMargin(child: View): Int {
        return (child.layoutParams as? MarginLayoutParams)?.leftMargin ?: 0
    }
    
    private fun getRightMargin(child: View): Int {
        return (child.layoutParams as? MarginLayoutParams)?.rightMargin ?: 0
    }
    
    private fun getTopMargin(child: View): Int {
        return (child.layoutParams as? MarginLayoutParams)?.topMargin ?: 0
    }
    
    private fun getBottomMargin(child: View): Int {
        return (child.layoutParams as? MarginLayoutParams)?.bottomMargin ?: 0
    }
    
    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }
    
    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }
    
    override fun generateLayoutParams(p: LayoutParams?): LayoutParams {
        return MarginLayoutParams(p)
    }
    
    override fun checkLayoutParams(p: LayoutParams?): Boolean {
        return p is MarginLayoutParams
    }
}