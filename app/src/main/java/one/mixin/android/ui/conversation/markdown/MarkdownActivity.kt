package one.mixin.android.ui.conversation.markdown

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.print.PrintPdfCallback
import android.print.printPdf
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import io.noties.markwon.recycler.MarkwonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.buffer
import okio.source
import one.mixin.android.R
import one.mixin.android.databinding.ActivityMarkdownBinding
import one.mixin.android.databinding.ViewMarkdownBinding
import one.mixin.android.extension.createPdfTemp
import one.mixin.android.extension.createPostTemp
import one.mixin.android.extension.getOtherPath
import one.mixin.android.extension.indeterminateProgressDialog
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.shareFile
import one.mixin.android.extension.toast
import one.mixin.android.ui.common.BaseActivity
import one.mixin.android.ui.conversation.link.LinkBottomSheetDialogFragment
import one.mixin.android.ui.forward.ForwardActivity
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.markdown.DefaultEntry
import one.mixin.android.util.markdown.MarkwonUtil
import one.mixin.android.util.markdown.SimpleEntry
import one.mixin.android.util.markdown.table.TableEntry
import one.mixin.android.vo.ForwardAction
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.widget.BottomSheet
import one.mixin.android.widget.WebControlView
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.node.FencedCodeBlock
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.AttributesCustomizer
import org.intellij.markdown.html.DUMMY_ATTRIBUTES_CUSTOMIZER
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.nio.charset.Charset

@AndroidEntryPoint
class MarkdownActivity : BaseActivity() {
    private lateinit var binding: ActivityMarkdownBinding
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMarkdownBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.control.mode = this.isNightMode()
        binding.control.callback = object : WebControlView.Callback {
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
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.adapter = adapter
        val markwon = MarkwonUtil.getMarkwon(
            this,
            { link ->
                LinkBottomSheetDialogFragment.newInstance(link)
                    .showNow(supportFragmentManager, LinkBottomSheetDialogFragment.TAG)
            },
            { link ->
                WebActivity.show(this, link, intent.getStringExtra(CONVERSATION_ID))
            }
        )
        val markdown = intent.getStringExtra(CONTENT) ?: return
        adapter.setMarkdown(markwon, markdown)
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("AutoDispose", "CheckResult")
    private fun showBottomSheet() {
        val builder = BottomSheet.Builder(this)
        val view = View.inflate(
            ContextThemeWrapper(this, R.style.Custom),
            R.layout.view_markdown,
            null
        )
        val viewBinding = ViewMarkdownBinding.bind(view)
        builder.setCustomView(view)
        val bottomSheet = builder.create()
        viewBinding.forward.setOnClickListener {
            val markdown = intent.getStringExtra(CONTENT) ?: return@setOnClickListener
            ForwardActivity.show(
                this,
                arrayListOf(ForwardMessage(ShareCategory.Post, markdown)),
                ForwardAction.App.Resultless()
            )
            bottomSheet.dismiss()
        }
        viewBinding.pdf.setOnClickListener {
            RxPermissions(this@MarkdownActivity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            savePdf {
                                bottomSheet.dismiss()
                            }
                        } else {
                            this@MarkdownActivity.openPermissionSetting()
                        }
                    },
                    {
                        toast(R.string.export_failure)
                    }
                )
        }
        viewBinding.save.setOnClickListener {
            RxPermissions(this)
                .request(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                .subscribe(
                    { granted ->
                        if (granted) {
                            savePost {
                                bottomSheet.dismiss()
                            }
                        } else {
                            openPermissionSetting()
                        }
                    },
                    {
                        toast(R.string.export_failure)
                    }
                )
        }
        bottomSheet.show()
    }

    private fun savePost(dismissAction: () -> Unit) {
        lifecycleScope.launch {
            val markdown = intent.getStringExtra(CONTENT) ?: return@launch
            try {
                withContext(Dispatchers.IO) {
                    val path = getOtherPath()
                    val file = path.createPostTemp()
                    file.outputStream().writer().use { writer ->
                        writer.write(markdown)
                    }
                    withContext(Dispatchers.Main) {
                        this@MarkdownActivity.shareFile(file, "text/*")
                    }
                }
            } catch (e: Exception) {
                toast(R.string.export_failure)
            }
            dismissAction()
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun savePdf(dismissAction: () -> Unit) = lifecycleScope.launch {
        val src = intent.getStringExtra(CONTENT) ?: return@launch

        val dialog = indeterminateProgressDialog(message = R.string.Please_wait_a_bit).apply {
            setCancelable(false)
        }
        dialog.show()
        binding.recyclerView.layoutManager?.smoothScrollToPosition(
            binding.recyclerView,
            null,
            binding.recyclerView.adapter?.itemCount ?: 0
        )

        val pdfFile = this@MarkdownActivity.getOtherPath()
            .createPdfTemp()
        val flavour = GFMFlavourDescriptor()
        val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(src)
        val body = HtmlGenerator(src, parsedTree, flavour, true)
            .generateHtml(HtmlTagRenderer(DUMMY_ATTRIBUTES_CUSTOMIZER, true))

        var pdfHtml = assets.open("pdf.html")
            .source()
            .buffer()
            .readByteString()
            .string(Charset.forName("utf-8"))
            .replace("#body-placeholder", body)
        if (isNightMode()) {
            pdfHtml = pdfHtml.replace("pdf-light.css", "pdf-dark.css")
        }

        printPdf(
            this@MarkdownActivity, pdfHtml, pdfFile,
            object : PrintPdfCallback {
                override fun onSuccess() {
                    this@MarkdownActivity.shareFile(pdfFile, "application/pdf")
                    dismissAction.invoke()
                    dialog.dismiss()
                }

                override fun onFailure(error: CharSequence?) {
                    dialog.dismiss()
                    toast(R.string.export_failure)
                }
            }
        )
    }

    inner class HtmlTagRenderer(
        private val customizer: AttributesCustomizer,
        private val includeSrcPositions: Boolean
    ) : HtmlGenerator.TagRenderer {
        override fun openTag(
            node: ASTNode,
            tagName: CharSequence,
            vararg attributes: CharSequence?,
            autoClose: Boolean
        ): CharSequence {
            return buildString {
                append("<$tagName")
                for (attribute in customizer.invoke(node, tagName, attributes.asIterable())) {
                    if (attribute != null) {
                        append(" $attribute")
                    }
                }
                if (includeSrcPositions) {
                    append(" ${HtmlGenerator.getSrcPosAttribute(node)}")
                }

                if (autoClose) {
                    append(" />")
                } else {
                    append(">")
                }
            }
        }

        override fun closeTag(tagName: CharSequence): CharSequence = "</$tagName>"

        override fun printHtml(html: CharSequence): CharSequence {
            return if (html == "<details>") {
                "<details open>"
            } else html
        }
    }

    companion object {
        private const val CONTENT = "content"
        private const val CONVERSATION_ID = "conversation_id"
        fun show(context: Context, content: String, conversationId: String? = null) {
            context.startActivity(
                Intent(context, MarkdownActivity::class.java).apply {
                    putExtra(CONTENT, content)
                    putExtra(CONVERSATION_ID, conversationId)
                }
            )
        }
    }
}
