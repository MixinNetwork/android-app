package one.mixin.android.widget.markdown

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import io.noties.markwon.editor.MarkwonEditor
import io.noties.markwon.editor.MarkwonEditorTextWatcher
import io.noties.markwon.editor.handler.EmphasisEditHandler
import io.noties.markwon.editor.handler.StrongEmphasisEditHandler
import one.mixin.android.widget.markdown.handler.CodeEditHandler
import one.mixin.android.widget.markdown.handler.StrikethroughEditHandler
import java.util.concurrent.Executors

open class MarkdownEditText : AppCompatEditText {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        val editor = MarkwonEditor.builder(getSimpleMarkwon(context))
            .useEditHandler(EmphasisEditHandler())
            .useEditHandler(StrongEmphasisEditHandler())
            .useEditHandler(StrikethroughEditHandler())
            .useEditHandler(CodeEditHandler())
            .build()

        addTextChangedListener(
            MarkwonEditorTextWatcher.withPreRender(
                editor,
                Executors.newCachedThreadPool(),
                this
            )
        )
    }
}
