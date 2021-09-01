package one.mixin.android.extension

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.text.inSpans
import androidx.core.text.scale
import io.noties.markwon.core.spans.CustomTypefaceSpan
import one.mixin.android.R

inline fun SpannableStringBuilder.font(
    typeface: Typeface? = null,
    builderAction: SpannableStringBuilder.() -> Unit
) = inSpans(CustomTypefaceSpan.create(typeface ?: Typeface.DEFAULT), builderAction = builderAction)

internal fun buildAmountSymbol(
    context: Context,
    amount: String,
    symbol: String,
    @ColorInt amountColor: Int,
    @ColorInt symbolColor: Int,
) = buildSpannedString {
    scale(3f) {
        font(
            ResourcesCompat.getFont(context, R.font.mixin_font)
        ) {
            color(amountColor) {
                append(amount)
            }
        }
    }
    append(" ")
    color(symbolColor) {
        append(symbol)
    }
}

internal fun buildBulletLines(context: Context, vararg lines: CharSequence): CharSequence {
    val builder = SpannableStringBuilder()
    lines.forEachIndexed { i, l ->
        if (l.isBlank()) return@forEachIndexed

        val line = "$l${if (i < lines.size - 1) "\n\n" else ""}"
        val spannable = SpannableString(line)
        val bulletSpan = BulletSpan(8.dp, context.colorFromAttribute(R.attr.text_minor))
        spannable.setSpan(bulletSpan, 0, spannable.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
        builder.append(spannable)
    }
    return builder
}
