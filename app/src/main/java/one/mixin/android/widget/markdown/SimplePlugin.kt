package one.mixin.android.widget.markdown

import android.text.Spannable
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.widget.TextView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.CoreProps
import io.noties.markwon.core.spans.CodeSpan
import io.noties.markwon.core.spans.EmphasisSpan
import io.noties.markwon.core.spans.OrderedListItemSpan
import io.noties.markwon.core.spans.StrongEmphasisSpan
import io.noties.markwon.core.spans.TextViewSpan
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.HardLineBreak
import org.commonmark.node.ListBlock
import org.commonmark.node.Node
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser

class SimplePlugin : AbstractMarkwonPlugin() {
    override fun configureParser(builder: Parser.Builder) {
        builder.extensions(setOf(StrikethroughExtension.create()))
    }

    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder.on(Text::class.java) { visitor, text ->
            val literal = text.literal
            visitor.builder().append(literal)
        }.on(StrongEmphasis::class.java) { visitor, strongEmphasis ->
            val length = visitor.length()
            visitor.visitChildren(strongEmphasis)
            visitor.setSpansForNodeOptional(strongEmphasis, length)
        }.on(Emphasis::class.java) { visitor, emphasis ->
            val length = visitor.length()
            visitor.visitChildren(emphasis)
            visitor.setSpansForNodeOptional(emphasis, length)
        }.on(Strikethrough::class.java) { visitor, strikethrough ->
            val length = visitor.length()
            visitor.visitChildren(strikethrough)
            visitor.setSpansForNodeOptional(strikethrough, length)
        }.on(Code::class.java) { visitor, code ->
            val length = visitor.length()
            visitor.builder()
                .append(code.literal)
            visitor.setSpansForNodeOptional(code, length)
        }.on(HardLineBreak::class.java) { visitor, _ ->
            visitor.ensureNewLine()
        }.on(SoftLineBreak::class.java) { visitor, _ ->
            visitor.forceNewLine()
        }.on(Paragraph::class.java) { visitor, paragraph ->
            val inTightList = isInTightList(paragraph)
            if (!inTightList) {
                visitor.blockStart(paragraph)
            }
            val length = visitor.length()
            visitor.visitChildren(paragraph)
            CoreProps.PARAGRAPH_IS_IN_TIGHT_LIST[visitor.renderProps()] = inTightList
            visitor.setSpansForNodeOptional(paragraph, length)
            if (!inTightList) {
                visitor.blockEnd(paragraph)
            }
        }
    }

    private fun isInTightList(paragraph: Paragraph): Boolean {
        val parent: Node? = paragraph.parent
        if (parent != null) {
            val gramps = parent.parent
            if (gramps is ListBlock) {
                return gramps.isTight
            }
        }
        return false
    }

    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
        builder
            .setFactory(StrongEmphasis::class.java) { _, _ -> StrongEmphasisSpan() }
            .setFactory(Emphasis::class.java) { _, _ -> EmphasisSpan() }
            .setFactory(Strikethrough::class.java) { _, _ -> StrikethroughSpan() }
            .setFactory(Code::class.java) { configuration, _ -> CodeSpan(configuration.theme()) }
    }

    override fun beforeSetText(textView: TextView, markdown: Spanned) {
        OrderedListItemSpan.measure(textView, markdown)

        if (markdown is Spannable) {
            TextViewSpan.applyTo(markdown, textView)
        }
    }
}
