package one.mixin.android.ui.conversation.markdown

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tbruyelle.rxpermissions2.RxPermissions
import io.noties.markwon.recycler.MarkwonAdapter
import io.noties.markwon.recycler.SimpleEntry
import io.noties.markwon.recycler.table.TableEntry
import kotlinx.android.synthetic.main.activity_markdown.*
import kotlinx.android.synthetic.main.view_markdown.view.*
import kotlinx.android.synthetic.main.view_web_bottom.view.forward
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.R
import one.mixin.android.extension.createPostTemp
import one.mixin.android.extension.getPublicDocumentPath
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.conversation.web.WebBottomSheetDialogFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.util.markdown.DefaultEntry
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.WebControlView
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.node.FencedCodeBlock

class MarkdownActivity : BaseActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_markdown)
        control.mode = isNightMode()
        control.callback = object : WebControlView.Callback {
            override fun onMoreClick() {
                showBottomSheet()
            }

            override fun onCloseClick() {
                finish()
            }
        }
        val adapter = MarkwonAdapter.builder(
            DefaultEntry()
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
        val markwon = MarkwonUtil.getMarkwon(this, { link ->
            LinkBottomSheetDialogFragment.newInstance(link)
                .showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
        }, { link ->
            WebBottomSheetDialogFragment.newInstance(link, intent.getStringExtra(CONVERSATION_ID))
                .showNow(supportFragmentManager, WebBottomSheetDialogFragment.TAG)
        })
        val markdown = intent.getStringExtra(CONTENT) ?: return
        adapter.setMarkdown(markwon, markdown)
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("AutoDispose")
    private fun showBottomSheet() {
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(
            ContextThemeWrapper(this, R.style.Custom),
            R.layout.view_markdown,
            null
        )
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        view.forward.setOnClickListener {
            val markdown = intent.getStringExtra(CONTENT) ?: return@setOnClickListener
            ForwardActivity.show(this, arrayListOf(ForwardMessage(ForwardCategory.POST.name, content = markdown)))
            bottomSheet.dismiss()
        }
        view.save.setOnClickListener {
            RxPermissions(this)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe({ granted ->
                    if (granted) {
                        savePost {
                            bottomSheet.dismiss()
                        }
                    } else {
                        openPermissionSetting()
                    }
                }, {
                })
        }
        bottomSheet.show()
    }

    private fun savePost(dismissAction: () -> Unit) {
        lifecycleScope.launch {
            val markdown = intent.getStringExtra(CONTENT) ?: return@launch
            try {
                withContext(Dispatchers.IO) {
                    val path = getPublicDocumentPath()
                    val file = path.createPostTemp()
                    file.outputStream().writer().use { writer ->
                        writer.write(markdown)
                    }
                    withContext(Dispatchers.Main) {
                        toast(getString(R.string.save_to, file.absolutePath))
                    }
                }
            } catch (e: Exception) {
                toast(R.string.save_failure)
            }
            dismissAction()
        }
    }

    companion object {
        private const val CONTENT = "content"
        private const val CONVERSATION_ID = "conversation_id"
        fun show(context: Context, content: String, conversationId: String? = null) {
            context.startActivity(Intent(context, MarkdownActivity::class.java).apply {
                putExtra(CONTENT, content)
                putExtra(CONVERSATION_ID, conversationId)
            })
        }
    }
}
