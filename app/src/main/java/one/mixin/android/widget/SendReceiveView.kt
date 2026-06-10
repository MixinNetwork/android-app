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
import androidx.core.content.ContextCompat
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
            item.layoutParams = LayoutParams(WALLET_HOME_ACTION_WIDTH_DP.dp, LayoutParams.WRAP_CONTENT)
            item.setPadding(4.dp, 8.dp, 4.dp, 8.dp)
            item.foreground = ContextCompat.getDrawable(context, R.drawable.mixin_ripple_8)
            item.collectViews(ImageView::class.java).forEach { image ->
                image.foreground = null
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
        if (visibleChildren.isEmpty()) {
            super.onLayout(changed, left, top, right, bottom)
            return
        }
        val contentWidth = (right - left - paddingLeft - paddingRight).coerceAtLeast(0)
        val childrenWidth = visibleChildren.sumOf { it.measuredWidth }
        val childTop = paddingTop
        val (startOffset, spacing) = walletHomeActionSpacing(
            contentWidth = contentWidth,
            childrenWidth = childrenWidth,
            actionCount = visibleChildren.size,
        )
        var childLeft = paddingLeft + startOffset
        visibleChildren.forEach { child ->
            child.layout(
                childLeft,
                childTop,
                childLeft + child.measuredWidth,
                childTop + child.measuredHeight,
            )
            childLeft += child.measuredWidth + spacing
        }
    }

    private fun walletHomeActionSpacing(
        contentWidth: Int,
        childrenWidth: Int,
        actionCount: Int,
    ): Pair<Int, Int> {
        val remainingWidth = (contentWidth - childrenWidth).coerceAtLeast(0)
        return when (actionCount) {
            WALLET_HOME_ACTION_COUNT_WITHOUT_BUY -> {
                val contentInset = minOf(WALLET_HOME_ACTION_ROW_CONTENT_INSET_DP.dp, remainingWidth / 2)
                val spacing = ((remainingWidth - contentInset * 2).coerceAtLeast(0)) / actionCount
                contentInset + spacing / 2 to spacing
            }
            WALLET_HOME_ACTION_COUNT_WITH_BUY -> {
                val outerPadding = WALLET_HOME_ACTION_ROW_HORIZONTAL_PADDING_DP.dp
                val spacing = ((remainingWidth - outerPadding * 2).coerceAtLeast(0)) / (actionCount - 1)
                outerPadding to spacing
            }
            else -> {
                val spacing = remainingWidth / actionCount
                spacing / 2 to spacing
            }
        }
    }

    private companion object {
        const val WALLET_HOME_ACTION_WIDTH_DP = 64
        const val WALLET_HOME_ACTION_COUNT_WITHOUT_BUY = 3
        const val WALLET_HOME_ACTION_COUNT_WITH_BUY = 4
        const val WALLET_HOME_ACTION_ROW_CONTENT_INSET_DP = 16
        const val WALLET_HOME_ACTION_ROW_HORIZONTAL_PADDING_DP = 9
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
