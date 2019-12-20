package one.mixin.android.ui.conversation.markdown

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.recycler.MarkwonAdapter
import io.noties.markwon.recycler.SimpleEntry
import io.noties.markwon.recycler.table.TableEntry
import kotlinx.android.synthetic.main.activity_markdown.*

import one.mixin.android.R
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.style.MarkwonUtil
import one.mixin.android.widget.WebControlView
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.node.FencedCodeBlock

class MarkdownActivity : BaseActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_markdown)
        web_control.mode = isNightMode()
        web_control.callback = object : WebControlView.Callback {
            override fun onMoreClick() {
                // Todo
            }

            override fun onCloseClick() {
                finish()
            }
        }
        val adapter = MarkwonAdapter.builder(
            R.layout.adapter_default_entry,
            R.id.text
        ).include(
            FencedCodeBlock::class.java,
            SimpleEntry.create(
                R.layout.item_markdown_code_block,
                R.id.text
            )
        ).include(
            TableBlock::class.java,
            TableEntry.create { builder: TableEntry.Builder ->
                builder
                    .tableLayout(R.layout.item_markdown_table_block, R.id.table_layout)
                    .textLayoutIsRoot(R.layout.item_markdown_cell)
            }
        ).build()
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter
        val markwon = MarkwonUtil.getSingle(this, isNightMode())
        val markdown = intent.getStringExtra(CONTENT) ?: return
        adapter.setMarkdown(markwon, markdown)
        adapter.notifyDataSetChanged()
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
