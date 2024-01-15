package one.mixin.android.extension

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.AbsoluteSizeSpan
import android.text.style.BulletSpan
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
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
    builderAction: SpannableStringBuilder.() -> Unit,
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
            ResourcesCompat.getFont(context, R.font.mixin_font),
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

internal fun buildBulletLines(
    context: Context,
    vararg lines: SpannableStringBuilder,
): CharSequence {
    val builder = SpannableStringBuilder()
    lines.filter { l -> l.isNotBlank() }.let { nonBlankLines ->
        nonBlankLines.forEachIndexed { i, l ->
            if (i < nonBlankLines.size - 1) {
                l.append("\n\n")
                l.setSpan(
                    AbsoluteSizeSpan(8, true),
                    l.length - 2,
                    l.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
            val bulletSpan = BulletSpan(8.dp, context.colorFromAttribute(R.attr.text_assist))
            l.setSpan(bulletSpan, 0, l.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
            builder.append(l)
        }
    }
    return builder
}

internal fun String.highLight(
    context: Context,
    target: String,
    ignoreCase: Boolean = true,
    @ColorInt color: Int = context.colorFromAttribute(R.attr.text_primary),
): SpannableStringBuilder {
    val spannable = SpannableStringBuilder(this)
    var index = indexOf(target, ignoreCase = ignoreCase)
    while (index != -1) {
        spannable.setSpan(
            TextAppearanceSpan(null, 0, 0, android.content.res.ColorStateList.valueOf(color), null),
            index,
            index + target.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        spannable.setSpan(StyleSpan(Typeface.BOLD), index, index + target.length, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
        index = indexOf(target, index + target.length, ignoreCase = ignoreCase)
    }
    return spannable
}
