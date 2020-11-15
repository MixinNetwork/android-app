package one.mixin.android.widget.markdown.handler

import android.text.Editable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import io.noties.markwon.core.spans.StrongEmphasisSpan
import io.noties.markwon.editor.AbstractEditHandler
import io.noties.markwon.editor.MarkwonEditorUtils
import io.noties.markwon.editor.PersistedSpans

class BoldEditHandler : AbstractEditHandler<StrongEmphasisSpan>() {
    override fun configurePersistedSpans(builder: PersistedSpans.Builder) {
        builder.persistSpan(Bold::class.java) { Bold() }
    }

    override fun handleMarkdownSpan(
        persistedSpans: PersistedSpans,
        editable: Editable,
        input: String,
        span: StrongEmphasisSpan,
        spanStart: Int,
        spanTextLength: Int
    ) {
        val match = MarkwonEditorUtils.findDelimited(input, spanStart, "**", "__")
        if (match != null) {
            editable.setSpan(
                persistedSpans.get(Bold::class.java),
                match.start(),
                match.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun markdownSpanType(): Class<StrongEmphasisSpan> {
        return StrongEmphasisSpan::class.java
    }

    internal class Bold : MetricAffectingSpan() {
        override fun updateDrawState(tp: TextPaint) {
            update(tp)
        }

        override fun updateMeasureState(textPaint: TextPaint) {
            update(textPaint)
        }

        private fun update(paint: TextPaint) {
            paint.isFakeBoldText = true
        }
    }
}
