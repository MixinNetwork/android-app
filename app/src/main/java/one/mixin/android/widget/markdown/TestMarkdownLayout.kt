package one.mixin.android.widget.markdown

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.widget.addTextChangedListener
import io.noties.markwon.Markwon
import io.noties.markwon.core.SimplePlugin
import kotlinx.android.synthetic.main.layout_test_markdown.view.*
import one.mixin.android.R

class TestMarkdownLayout : LinearLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.layout_test_markdown, this, true)
        val markwon = Markwon.builderNoCore(context).usePlugin(SimplePlugin.create())
            .build()
        markdown.addTextChangedListener {
            it?.let {
                tv.text = markwon.toMarkdown(it.toString())
            }
        }
    }
}
