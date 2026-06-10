package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class MiddleMultilineTextView(context: Context, attributeSet: AttributeSet) : AppCompatTextView(context, attributeSet) {
    companion object {
        private const val SYMBOL = "..."
        private const val SYMBOL_LENGTH = SYMBOL.length
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (maxLines > 1) {
            val originLength = text.length
            val visibleLength = getVisibleLength()
            if (originLength > visibleLength) {
                text = trimText(text.toString(), visibleLength - SYMBOL_LENGTH)
            }
        }
    }

    private fun getVisibleLength(): Int {
        val start = layout.getLineStart(0)
        val end = layout.getLineEnd(maxLines - 1)
        return text.toString().substring(start, end).length
    }

    private fun trimText(
        string: String,
        maxLength: Int,
    ): String {
        if (maxLength < 1) {
            return string
        } else if (string.length <= maxLength) {
            return string
        } else if (maxLength == 1) {
            return string.substring(0, 1) + SYMBOL
        }
        val mid = Math.ceil(string.length / 2.0).toInt()
        val removeLen = string.length - maxLength
        val left = Math.ceil(removeLen / 2.0).toInt()
        val right = removeLen - left
        return string.substring(0, mid - left) + SYMBOL + string.substring(mid + right)
    }
}
