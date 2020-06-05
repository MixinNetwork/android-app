package one.mixin.android.widget

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ScaleXSpan
import android.util.AttributeSet
import android.util.Log
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import one.mixin.android.BuildConfig
import one.mixin.android.R

/**
 * KerningTextView is a special [TextView] which allows adjusting
 * the spacing between characters in a piece of text AKA Kerning.
 *
 *
 * You can use the `#setKerningFactor()` method to adjust the kerning programmatically,
 * but the more common approach would be to use `ktv_spacing` attribute in xml.
 */
class KerningTextView : AppCompatTextView {

    private val TAG = javaClass.simpleName

    private var kerningFactor = NO_KERNING
    private var originalText: CharSequence? = null

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        val originalTypedArray = context.obtainStyledAttributes(attrs, intArrayOf(android.R.attr.text))
        val currentTypedArray = context.obtainStyledAttributes(attrs, R.styleable.KerningTextView, 0, defStyle)

        try {
            kerningFactor = currentTypedArray.getFloat(R.styleable.KerningTextView_kv_spacing, NO_KERNING)
            originalText = originalTypedArray.getText(0)
        } finally {
            originalTypedArray.recycle()
            currentTypedArray.recycle()
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, String.format("Kerning Factor: %s", kerningFactor))
            Log.d(TAG, String.format("Original Text: %s", originalText))
        }

        applyKerning()
    }

    /**
     * Programmatically get the value of the `kerningFactor`
     */
    fun getKerningFactor(): Float {
        return kerningFactor
    }

    /**
     * Programmatically set the value of the `kerningFactor`
     */
    fun setKerningFactor(kerningFactor: Float) {
        this.kerningFactor = kerningFactor
        applyKerning()
    }

    override fun setText(text: CharSequence, type: BufferType) {
        originalText = text
        applyKerning()
    }

    override fun getText(): CharSequence? {
        return originalText
    }

    private fun applyKerning() {
        if (originalText == null) {
            return
        }

        val builder = StringBuilder()
        for (i in 0 until originalText!!.length) {
            builder.append(originalText!![i])
            if (i + 1 < originalText!!.length) {
                builder.append("\u00A0")
            }
        }

        val finalText = SpannableString(builder.toString())
        if (builder.toString().length > 1) {
            var i = 1
            while (i < builder.toString().length) {
                finalText.setSpan(
                    ScaleXSpan(kerningFactor / 10),
                    i,
                    i + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                i += 2
            }
        }
        super.setText(finalText, BufferType.SPANNABLE)
    }

    /**
     * Default kerning values which can be used for convenience
     */
    companion object {
        const val NO_KERNING = 0f
        const val SMALL = 1f
        const val MEDIUM = 4f
        const val LARGE = 6f
    }
}
