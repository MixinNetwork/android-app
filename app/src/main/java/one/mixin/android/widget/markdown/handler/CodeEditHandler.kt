package one.mixin.android.widget.markdown.handler

import android.text.Editable
import android.text.Spanned
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.editor.EditHandler
import io.noties.markwon.editor.MarkwonEditorUtils
import io.noties.markwon.editor.PersistedSpans
import one.mixin.android.widget.markdown.RoundedBackgroundSpan

class CodeEditHandler : EditHandler<RoundedBackgroundSpan> {
    private lateinit var theme: MarkwonTheme
    override fun init(markwon: Markwon) {
        theme = markwon.configuration().theme()
    }

    override fun configurePersistedSpans(builder: PersistedSpans.Builder) {
        builder.persistSpan(RoundedBackgroundSpan::class.java) {
            RoundedBackgroundSpan(
                theme
            )
        }
    }

    override fun handleMarkdownSpan(
        persistedSpans: PersistedSpans,
        editable: Editable,
        input: String,
        span: RoundedBackgroundSpan,
        spanStart: Int,
        spanTextLength: Int
    ) {
        val match = MarkwonEditorUtils.findDelimited(input, spanStart, "`")
        if (match != null) {
            editable.setSpan(
                persistedSpans.get(RoundedBackgroundSpan::class.java),
                match.start() + 1,
                match.end() - 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    override fun markdownSpanType(): Class<RoundedBackgroundSpan> {
        return RoundedBackgroundSpan::class.java
    }
}
