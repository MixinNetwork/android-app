package one.mixin.android.extension

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
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
            ResourcesCompat.getFont(context, R.font.mixin_condensed)
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
