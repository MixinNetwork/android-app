package one.mixin.android.ui.wallet

import android.content.Context
import android.os.Build
import android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.View.MeasureSpec
import androidx.appcompat.widget.AppCompatTextView
import one.mixin.android.R
import one.mixin.android.extension.colorAttr

class MarketDescriptionTextView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : AppCompatTextView(context, attrs) {
        private val collapsedMaxLines = maxLines.takeIf { it > 0 && it < Int.MAX_VALUE } ?: DEFAULT_COLLAPSED_MAX_LINES
        private var descriptionText: CharSequence = ""
        private var expanded = false
        private var expandable = false

        init {
            maxLines = Int.MAX_VALUE
            ellipsize = null
            setOnClickListener {
                if (expandable) {
                    expanded = !expanded
                    requestLayout()
                }
            }
        }

        fun setMarketDescription(description: String) {
            descriptionText = description.normalizeLineBreaks().trim()
            expanded = false
            requestLayout()
        }

        override fun onMeasure(
            widthMeasureSpec: Int,
            heightMeasureSpec: Int,
        ) {
            val textWidth = MeasureSpec.getSize(widthMeasureSpec) - compoundPaddingStart - compoundPaddingEnd
            updateDisplayedText(textWidth)
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }

        private fun updateDisplayedText(textWidth: Int) {
            if (textWidth <= 0 || descriptionText.isBlank()) {
                expandable = false
                isClickable = false
                text = ""
                return
            }
            expandable = buildDescriptionLayout(textWidth).lineCount > collapsedMaxLines
            isClickable = expandable
            text = if (expandable && !expanded) {
                buildCollapsedText(textWidth)
            } else {
                descriptionText
            }
        }

        private fun buildCollapsedText(textWidth: Int): CharSequence {
            var low = 0
            var high = descriptionText.length
            var best = 0
            while (low <= high) {
                val mid = (low + high) / 2
                val candidate = buildCollapsedCandidate(mid)
                if (buildDescriptionLayout(textWidth, candidate).lineCount <= collapsedMaxLines) {
                    best = mid
                    low = mid + 1
                } else {
                    high = mid - 1
                }
            }
            return buildCollapsedCandidate(best)
        }

        private fun buildCollapsedCandidate(textEnd: Int): SpannableStringBuilder {
            var visibleText = descriptionText
                .take(textEnd)
                .trimEnd { it.isWhitespace() || it == '.' || it == Typography.ellipsis }
            if (textEnd < descriptionText.length &&
                visibleText.lastOrNull()?.isLetterOrDigit() == true &&
                descriptionText.getOrNull(textEnd)?.isLetterOrDigit() == true
            ) {
                val wordBoundary = visibleText.indexOfLast { it.isWhitespace() }
                if (wordBoundary >= 0) {
                    visibleText = visibleText.substring(0, wordBoundary).trimEnd()
                }
            }
            val moreText = context.getString(R.string.More).lowercase()
            val suffix = "${Typography.ellipsis} $moreText"
            val span = SpannableStringBuilder()
                .append(visibleText)
                .append(suffix)
            val moreStart = span.length - moreText.length
            span.setSpan(
                ForegroundColorSpan(context.colorAttr(R.attr.text_blue)),
                moreStart,
                span.length,
                SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            return span
        }

        private fun buildDescriptionLayout(textWidth: Int): StaticLayout {
            return buildDescriptionLayout(textWidth, descriptionText)
        }

        private fun buildDescriptionLayout(
            textWidth: Int,
            text: CharSequence,
        ): StaticLayout {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                StaticLayout.Builder
                    .obtain(text, 0, text.length, paint, textWidth)
                    .setIncludePad(includeFontPadding)
                    .setLineSpacing(lineSpacingExtra, lineSpacingMultiplier)
                    .setBreakStrategy(breakStrategy)
                    .setHyphenationFrequency(hyphenationFrequency)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(
                    text,
                    paint,
                    textWidth,
                    layout?.alignment ?: android.text.Layout.Alignment.ALIGN_NORMAL,
                    lineSpacingMultiplier,
                    lineSpacingExtra,
                    includeFontPadding,
                )
            }
        }

        private fun String.normalizeLineBreaks(): String =
            replace("\r\n", "\n")
                .replace('\r', '\n')

        companion object {
            private const val DEFAULT_COLLAPSED_MAX_LINES = 3
        }
    }
