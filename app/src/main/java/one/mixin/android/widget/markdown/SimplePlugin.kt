package io.noties.markwon.core

import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.StrikethroughSpan
import android.widget.TextView
import androidx.annotation.VisibleForTesting
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.factory.BlockQuoteSpanFactory
import io.noties.markwon.core.factory.CodeBlockSpanFactory
import io.noties.markwon.core.factory.CodeSpanFactory
import io.noties.markwon.core.factory.EmphasisSpanFactory
import io.noties.markwon.core.factory.HeadingSpanFactory
import io.noties.markwon.core.factory.LinkSpanFactory
import io.noties.markwon.core.factory.ListItemSpanFactory
import io.noties.markwon.core.factory.StrongEmphasisSpanFactory
import io.noties.markwon.core.factory.ThematicBreakSpanFactory
import io.noties.markwon.core.spans.CodeSpan
import io.noties.markwon.core.spans.EmphasisSpan
import io.noties.markwon.core.spans.OrderedListItemSpan
import io.noties.markwon.core.spans.StrongEmphasisSpan
import io.noties.markwon.core.spans.TextViewSpan
import io.noties.markwon.image.ImageProps
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.Block
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListBlock
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
import java.util.Arrays
import java.util.HashSet

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
        }
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

    private fun text(builder: MarkwonVisitor.Builder) {
        builder.on(Text::class.java) { visitor, text ->
            val literal = text.literal
            visitor.builder().append(literal)
        }
    }

    companion object {
        fun create(): SimplePlugin {
            return SimplePlugin()
        }
    }
}