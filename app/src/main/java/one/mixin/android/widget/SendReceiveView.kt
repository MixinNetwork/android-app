package one.mixin.android.widget

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import one.mixin.android.R
import one.mixin.android.databinding.ViewSendReceiveBinding
import one.mixin.android.extension.colorAttr
import one.mixin.android.extension.dp

class SendReceiveView : LinearLayoutCompat {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        binding = ViewSendReceiveBinding.inflate(LayoutInflater.from(context), this)
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SendReceiveView)
        walletHomeStyle = typedArray.getBoolean(R.styleable.SendReceiveView_walletHomeStyle, false)
        typedArray.recycle()
        weightSum = 3f
        if (walletHomeStyle) {
            applyWalletHomeStyle()
        }
    }

    private val binding: ViewSendReceiveBinding
    private val walletHomeStyle: Boolean
    val send get() = binding.send
    val receive get() = binding.receive
    val swap get() = binding.swap
    val swapBadge get() = binding.swapBadge
    val buy get() = binding.buy
    val buyBadge get() = binding.buyBadge

    fun enableBuy() {
        this.weightSum = 4f
        binding.buy.visibility = VISIBLE
        if (walletHomeStyle) {
            applyWalletHomeStyle()
        }
    }

    private fun applyWalletHomeStyle() {
        weightSum = 0f
        listOf(binding.buy, binding.receive, binding.send, binding.swap).forEach { item ->
            item.layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            item.setPadding(4.dp, 8.dp, 4.dp, 8.dp)
            item.collectViews(ImageView::class.java).forEach { image ->
                image.layoutParams = image.layoutParams.apply {
                    width = 42.dp
                    height = 42.dp
                }
            }
            item.collectViews(TextView::class.java).forEach { text ->
                text.setTextColor(context.colorAttr(R.attr.text_minor))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    text.typeface = Typeface.create(text.typeface, 500)
                } else {
                    text.setTypeface(text.typeface, Typeface.BOLD)
                }
                (text.layoutParams as? MarginLayoutParams)?.topMargin = 11.dp
            }
        }
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        if (!walletHomeStyle) {
            super.onLayout(changed, left, top, right, bottom)
            return
        }
        val visibleChildren = (0 until childCount)
            .map { getChildAt(it) }
            .filter { it.visibility != View.GONE }
        if (visibleChildren.size <= 1) {
            super.onLayout(changed, left, top, right, bottom)
            return
        }
        val contentWidth = right - left - paddingLeft - paddingRight
        val totalChildWidth = visibleChildren.sumOf { it.measuredWidth }
        val gap = ((contentWidth - totalChildWidth).coerceAtLeast(0)).toFloat() / (visibleChildren.size - 1)
        var childLeft = paddingLeft.toFloat()
        visibleChildren.forEach { child ->
            val childTop = paddingTop
            child.layout(
                childLeft.toInt(),
                childTop,
                childLeft.toInt() + child.measuredWidth,
                childTop + child.measuredHeight,
            )
            childLeft += child.measuredWidth + gap
        }
    }

    private fun <T : View> View.collectViews(clazz: Class<T>): List<T> {
        val result = mutableListOf<T>()
        if (clazz.isInstance(this)) result += clazz.cast(this)
        if (this is ViewGroup) {
            for (i in 0 until childCount) {
                result += getChildAt(i).collectViews(clazz)
            }
        }
        return result
    }
}
