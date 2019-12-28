package one.mixin.android.ui.conversation.markdown

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import io.noties.markwon.recycler.MarkwonAdapter
import io.noties.markwon.recycler.SimpleEntry
import io.noties.markwon.recycler.table.TableEntry
import kotlinx.android.synthetic.main.activity_markdown.*
import one.mixin.android.R
import one.mixin.android.ui.common.BasePullCollapsibleActivity
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.widget.WebControlView
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.node.FencedCodeBlock

class MarkdownActivity : BasePullCollapsibleActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_markdown)
        val position = intent.getIntArrayExtra(POSISTION)
        if (position != null && position.size == 4) {
            val rect = Rect(position[0], position[1], position[2], position[3])
            expandFrom(rect)
        } else {
            expandFromTop()
        }
        control.mode = isNightMode()
        control.callback = object : WebControlView.Callback {
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
        recycler_view.layoutManager = LinearLayoutManager(this)
        recycler_view.setHasFixedSize(true)
        recycler_view.adapter = adapter
        val markwon = MarkwonUtil.getMarkwon(this) { link ->
            LinkBottomSheetDialogFragment.newInstance(link)
                .showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        }
        val markdown = intent.getStringExtra(CONTENT) ?: return
        adapter.setMarkdown(markwon, markdown)
        adapter.notifyDataSetChanged()
    }

    companion object {
        private const val CONTENT = "content"
        private const val POSISTION = "position"
        fun show(context: Context, content: String, position: IntArray? = null) {
            context.startActivity(Intent(context, MarkdownActivity::class.java).apply {
                putExtra(CONTENT, content)
                putExtra(POSISTION, position)
            })
        }
    }
}
