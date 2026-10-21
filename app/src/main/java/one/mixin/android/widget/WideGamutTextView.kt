@file:Suppress("OVERRIDE_DEPRECATION")

package one.mixin.android.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Editable
import android.text.PrecomputedText
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.util.AttributeSet
import android.content.res.ColorStateList
import androidx.core.text.PrecomputedTextCompat
import com.google.android.material.textview.MaterialTextView

class WideGamutTextView(context: Context, attrs: AttributeSet?) : MaterialTextView(context, attrs) {
    private var applyingQuoteSpan = false

    override fun setText(text: CharSequence?, type: BufferType?) {
        if (applyingQuoteSpan || type == BufferType.EDITABLE || text is Editable) {
            super.setText(text, type)
            return
        }
        val styled = styleQuote(text)
        val buffer = if (styled is Spannable && styled !== text) BufferType.SPANNABLE else type
        super.setText(styled, buffer)
    }

    override fun setTextColor(color: Int) {
        super.setTextColor(color)
        restyleCurrentText()
    }

    override fun setTextColor(colors: ColorStateList) {
        super.setTextColor(colors)
        restyleCurrentText()
    }

    override fun setBackground(background: Drawable?) {
        super.setBackground(background?.quoteWideGamutBackground())
    }

    private fun restyleCurrentText() {
        if (applyingQuoteSpan) return
        val current = text ?: return
        if (current is Editable) return
        val styled = styleQuote(current)
        if (styled === current) {
            invalidate()
            return
        }
        applyingQuoteSpan = true
        try {
            super.setText(styled, BufferType.SPANNABLE)
        } finally {
            applyingQuoteSpan = false
        }
    }

    private fun styleQuote(text: CharSequence?): CharSequence? {
        if (text == null) return null
        val colorLong = if (QuoteColors.supportsWideGamut(context)) {
            QuoteColors.colorLongOrNull(currentTextColor)
        } else {
            null
        }
        if (colorLong == null) {
            return removeUpgradeSpans(text)
        }
        if (text.isEmpty()) return text
        val target = mutableSpannable(text)
        val spans = target.getSpans(0, target.length, WideGamutColorSpan::class.java)
        val expectedRgb = currentTextColor and 0xFFFFFF
        val existing = spans.firstOrNull { it.upgrade }
        val already = existing != null &&
            existing.packedColor == colorLong &&
            target.getSpanStart(existing) == 0 &&
            target.getSpanEnd(existing) == target.length &&
            target === text
        if (already) return text
        spans.filter { it.upgrade }.forEach { target.removeSpan(it) }
        target.setSpan(
            WideGamutColorSpan(colorLong, expectedRgb, upgrade = true),
            0,
            target.length,
            Spanned.SPAN_INCLUSIVE_INCLUSIVE,
        )
        return target
    }

    private fun removeUpgradeSpans(text: CharSequence): CharSequence {
        if (text !is Spanned) return text
        val spans = text.getSpans(0, text.length, WideGamutColorSpan::class.java).filter { it.upgrade }
        if (spans.isEmpty()) return text
        val target = mutableSpannable(text)
        spans.forEach { target.removeSpan(it) }
        return target
    }

    private fun mutableSpannable(text: CharSequence): Spannable {
        if (text is Spannable && text !is PrecomputedText && text !is PrecomputedTextCompat) {
            return text
        }
        return SpannableString(text)
    }

    private fun Drawable.quoteWideGamutBackground(): Drawable {
        if (!QuoteColors.supportsWideGamut(context)) return this
        if (this is WideGamutQuoteDrawable) return this
        val argb = when (this) {
            is ColorDrawable -> color
            is GradientDrawable -> {
                if (shape != GradientDrawable.RECTANGLE) return this
                color?.defaultColor ?: return this
            }
            else -> return this
        }
        val colorLong = QuoteColors.colorLongOrNull(argb) ?: return this
        val radius = (this as? GradientDrawable)?.cornerRadius ?: 0f
        val padding = Rect()
        val hasPadding = getPadding(padding)
        return WideGamutQuoteDrawable(
            colorLong,
            radius,
            if (hasPadding) Rect(padding) else null,
        )
    }
}

private class WideGamutQuoteDrawable(
    colorLong: Long,
    private val cornerRadiusPx: Float,
    private val contentPadding: Rect?,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    init {
        if (Build.VERSION.SDK_INT >= 29) {
            paint.setColor(colorLong)
        }
    }

    override fun draw(canvas: Canvas) {
        rect.set(bounds)
        if (cornerRadiusPx > 0f) {
            canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)
        } else {
            canvas.drawRect(rect, paint)
        }
    }

    override fun getPadding(padding: Rect): Boolean {
        val source = contentPadding ?: return false
        padding.set(source)
        return true
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
