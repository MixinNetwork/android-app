package one.mixin.android.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import one.mixin.android.R
import androidx.core.content.withStyledAttributes

class AddressTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var minTextSizePx: Float = 0f
    private var maxTextSizePx: Float = 0f

    // The text length at which the font size starts to shrink.
    private val startShrinkLength = 100

    // The text length at which the font size reaches its minimum.
    private val endShrinkLength = 160

    init {
        context.withStyledAttributes(attrs, R.styleable.AutosizeTextView, defStyleAttr, 0) {
            minTextSizePx = getDimension(R.styleable.AutosizeTextView_minTextSize, 10f * resources.displayMetrics.scaledDensity)
            maxTextSizePx = getDimension(R.styleable.AutosizeTextView_maxTextSize, 14f * resources.displayMetrics.scaledDensity)
        }
    }

    private fun adjustTextSize(text: CharSequence?) {
        val length = text?.length ?: 0

        val newSizePx = when {
            length < startShrinkLength -> maxTextSizePx
            length >= endShrinkLength -> minTextSizePx
            else -> {
                // Linear interpolation
                val slope = (minTextSizePx - maxTextSizePx) / (endShrinkLength - startShrinkLength)
                maxTextSizePx + slope * (length - startShrinkLength)
            }
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, newSizePx)
    }

    override fun onTextChanged(
        text: CharSequence?,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int,
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        adjustTextSize(text)
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        adjustTextSize(text)
    }
}
