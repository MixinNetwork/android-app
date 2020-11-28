package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import one.mixin.android.databinding.ViewFlagBinding
import one.mixin.android.extension.dp
import one.mixin.android.extension.translationY
import kotlin.math.max

class FlagLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ViewGroup(context, attrs, defStyle) {

    private val binding = ViewFlagBinding.inflate(LayoutInflater.from(context), this)
    val downFlagLayout get() = binding.downFlagLayout
    val mentionFlagLayout get() = binding.mentionFlagLayout

    private val space by lazy {
        3.dp
    }

    var bottomFlag = false
        set(value) {
            if (field != value) {
                binding.downFlagLayout.isVisible = value
                field = value
                update()
            }
        }

    var bottomCountFlag = false
        set(value) {
            if (field != value) {
                binding.downUnread.isVisible = value
                field = value
                update()
            }
        }

    var mentionCount = 0
        set(value) {
            if (field != value) {
                binding.mentionFlagLayout.isVisible = value > 0
                binding.mentionCount.text = "$value"
                field = value
                update()
            }
        }

    var unreadCount = 0
        set(value) {
            if (field != value) {
                field = value
                binding.downUnread.text = "$value"
                update()
            }
        }

    private fun update() {
        requestLayout()
        if (!bottomCountFlag && !bottomFlag && mentionCount == 0) {
            hide()
        } else {
            show()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var bottom = measuredHeight - paddingBottom
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if ((i == 1 && bottomFlag) || (i == 0 && mentionCount != 0)) {
                child.layout(paddingStart, bottom - child.measuredHeight, paddingStart + child.measuredWidth, bottom)
                bottom -= child.measuredHeight
                if (i == 1) {
                    bottom -= space
                }
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        var width = 0
        var height = 0
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            width = max(child.measuredWidth, width)
            height += child.measuredHeight
        }
        setMeasuredDimension(width + paddingStart + paddingEnd + space, height + paddingTop + paddingBottom)
    }

    private fun show() {
        if (this.translationY != 0f)
            translationY(0f, 100)
    }

    private fun hide() {
        if (this.translationY == 0f)
            translationY(130.dp.toFloat(), 100)
    }
}
