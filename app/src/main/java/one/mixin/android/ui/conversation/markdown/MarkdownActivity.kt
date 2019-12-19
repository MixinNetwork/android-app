package one.mixin.android.ui.conversation.markdown

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_markdown.*
import one.mixin.android.R
import one.mixin.android.ui.style.MarkwonUtil

class MarkdownActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_markdown)
        val markdown = intent.getStringExtra(CONTENT) ?: return
        MarkwonUtil.getSingle().setMarkdown(tv, markdown)
    }

    companion object {
        private const val CONTENT = "content"
        fun show(context: Context, content: String) {
            context.startActivity(Intent(context, MarkdownActivity::class.java).apply {
                putExtra(CONTENT, content)
            })
        }
    }
}
