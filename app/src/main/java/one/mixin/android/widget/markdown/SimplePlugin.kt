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
import io.noties.markwon.core.spans.OrderedListItemSpan
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
import java.util.ArrayList
import java.util.Arrays
import java.util.HashSet

class SimplePlugin : AbstractMarkwonPlugin() {
    override fun configureParser(builder: Parser.Builder) {
        builder.extensions(setOf(StrikethroughExtension.create()))
    }

    interface OnTextAddedListener {

        fun onTextAdded(visitor: MarkwonVisitor, text: String, start: Int)
    }

    private val onTextAddedListeners: MutableList<OnTextAddedListener> = ArrayList(0)

    private var hasExplicitMovementMethod = false

    fun hasExplicitMovementMethod(hasExplicitMovementMethod: Boolean): SimplePlugin {
        this.hasExplicitMovementMethod = hasExplicitMovementMethod
        return this
    }

    fun addOnTextAddedListener(onTextAddedListener: OnTextAddedListener): SimplePlugin {
        onTextAddedListeners.add(onTextAddedListener)
        return this
    }

    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        text(builder)
        strongEmphasis(builder)
        emphasis(builder)
        blockQuote(builder)
        code(builder)
        fencedCodeBlock(builder)
        indentedCodeBlock(builder)
        image(builder)
        bulletList(builder)
        orderedList(builder)
        listItem(builder)
        thematicBreak(builder)
        heading(builder)
        softLineBreak(builder)
        hardLineBreak(builder)
        paragraph(builder)
        link(builder)
        builder.on(Strikethrough::class.java) { visitor, strikethrough ->
            val length = visitor.length()
            visitor.visitChildren(strikethrough)
            visitor.setSpansForNodeOptional(strikethrough, length)
        }
    }

    override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {

        val codeBlockSpanFactory = CodeBlockSpanFactory()
        builder
            .setFactory(StrongEmphasis::class.java, StrongEmphasisSpanFactory())
            .setFactory(Emphasis::class.java, EmphasisSpanFactory())
            .setFactory(BlockQuote::class.java, BlockQuoteSpanFactory())
            .setFactory(Code::class.java, CodeSpanFactory())
            .setFactory(FencedCodeBlock::class.java, codeBlockSpanFactory)
            .setFactory(IndentedCodeBlock::class.java, codeBlockSpanFactory)
            .setFactory(ListItem::class.java, ListItemSpanFactory())
            .setFactory(Heading::class.java, HeadingSpanFactory())
            .setFactory(Link::class.java, LinkSpanFactory())
            .setFactory(ThematicBreak::class.java, ThematicBreakSpanFactory())
            .setFactory(Strikethrough::class.java) { _, _ -> StrikethroughSpan() }
    }

    override fun beforeSetText(textView: TextView, markdown: Spanned) {
        OrderedListItemSpan.measure(textView, markdown)

        if (markdown is Spannable) {
            TextViewSpan.applyTo(markdown, textView)
        }
    }

    override fun afterSetText(textView: TextView) {
        if (!hasExplicitMovementMethod && textView.movementMethod == null) {
            textView.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun text(builder: MarkwonVisitor.Builder) {
        builder.on(Text::class.java) { visitor, text ->
            val literal = text.literal
            visitor.builder().append(literal)

            if (!onTextAddedListeners.isEmpty()) {
                val length = visitor.length() - literal.length
                for (onTextAddedListener in onTextAddedListeners) {
                    onTextAddedListener.onTextAdded(visitor, literal, length)
                }
            }
        }
    }

    companion object {
        fun create(): SimplePlugin {
            return SimplePlugin()
        }

        fun enabledBlockTypes(): Set<Class<out Block?>> {
            return HashSet(
                Arrays.asList(
                    BlockQuote::class.java,
                    Heading::class.java,
                    FencedCodeBlock::class.java,
                    HtmlBlock::class.java,
                    ThematicBreak::class.java,
                    ListBlock::class.java,
                    IndentedCodeBlock::class.java
                )
            )
        }

        private fun strongEmphasis(builder: MarkwonVisitor.Builder) {
            builder.on(StrongEmphasis::class.java) { visitor, strongEmphasis ->
                val length = visitor.length()
                visitor.visitChildren(strongEmphasis)
                visitor.setSpansForNodeOptional(strongEmphasis, length)
            }
        }

        private fun emphasis(builder: MarkwonVisitor.Builder) {
            builder.on(Emphasis::class.java) { visitor, emphasis ->
                val length = visitor.length()
                visitor.visitChildren(emphasis)
                visitor.setSpansForNodeOptional(emphasis, length)
            }
        }

        private fun blockQuote(builder: MarkwonVisitor.Builder) {
            builder.on(BlockQuote::class.java) { visitor, blockQuote ->
                visitor.blockStart(blockQuote)
                val length = visitor.length()
                visitor.visitChildren(blockQuote)
                visitor.setSpansForNodeOptional(blockQuote, length)
                visitor.blockEnd(blockQuote)
            }
        }

        private fun code(builder: MarkwonVisitor.Builder) {
            builder.on(Code::class.java) { visitor, code ->
                val length = visitor.length()

                visitor.builder()
                    .append('\u00a0')
                    .append(code.literal)
                    .append('\u00a0')
                visitor.setSpansForNodeOptional(code, length)
            }
        }

        private fun fencedCodeBlock(builder: MarkwonVisitor.Builder) {
            builder.on(
                FencedCodeBlock::class.java
            ) { visitor, fencedCodeBlock ->
                visitCodeBlock(
                    visitor,
                    fencedCodeBlock.info,
                    fencedCodeBlock.literal,
                    fencedCodeBlock
                )
            }
        }

        private fun indentedCodeBlock(builder: MarkwonVisitor.Builder) {
            builder.on(
                IndentedCodeBlock::class.java
            ) { visitor, indentedCodeBlock ->
                visitCodeBlock(
                    visitor,
                    null,
                    indentedCodeBlock.literal,
                    indentedCodeBlock
                )
            }
        }

        private fun image(builder: MarkwonVisitor.Builder) {
            builder.on(
                Image::class.java,
                MarkwonVisitor.NodeVisitor { visitor, image -> // if there is no image spanFactory, ignore
                    val spanFactory = visitor.configuration().spansFactory().get(
                        Image::class.java
                    )
                    if (spanFactory == null) {
                        visitor.visitChildren(image)
                        return@NodeVisitor
                    }
                    val length = visitor.length()
                    visitor.visitChildren(image)

                    // we must check if anything _was_ added, as we need at least one char to render
                    if (length == visitor.length()) {
                        visitor.builder().append('\uFFFC')
                    }
                    val configuration = visitor.configuration()
                    val parent = image.parent
                    val link = parent is Link
                    val destination = configuration
                        .imageDestinationProcessor()
                        .process(image.destination)
                    val props = visitor.renderProps()

                    ImageProps.DESTINATION[props] = destination
                    ImageProps.REPLACEMENT_TEXT_IS_LINK[props] = link
                    ImageProps.IMAGE_SIZE[props] = null
                    visitor.setSpans(length, spanFactory.getSpans(configuration, props))
                })
        }

        @VisibleForTesting
        fun visitCodeBlock(
            visitor: MarkwonVisitor,
            info: String?,
            code: String,
            node: Node
        ) {
            visitor.blockStart(node)
            val length = visitor.length()
            visitor.builder()
                .append('\u00a0').append('\n')
                .append(visitor.configuration().syntaxHighlight().highlight(info, code))
            visitor.ensureNewLine()
            visitor.builder().append('\u00a0')

            CoreProps.CODE_BLOCK_INFO[visitor.renderProps()] = info
            visitor.setSpansForNodeOptional(node, length)
            visitor.blockEnd(node)
        }

        private fun bulletList(builder: MarkwonVisitor.Builder) {
            builder.on(BulletList::class.java, SimpleBlockNodeVisitor())
        }

        private fun orderedList(builder: MarkwonVisitor.Builder) {
            builder.on(OrderedList::class.java, SimpleBlockNodeVisitor())
        }

        private fun listItem(builder: MarkwonVisitor.Builder) {
            builder.on(ListItem::class.java) { visitor, listItem ->
                val length = visitor.length()

                visitor.visitChildren(listItem)
                val parent: Node = listItem.parent
                if (parent is OrderedList) {
                    val start = parent.startNumber
                    CoreProps.LIST_ITEM_TYPE[visitor.renderProps()] = CoreProps.ListItemType.ORDERED
                    CoreProps.ORDERED_LIST_ITEM_NUMBER[visitor.renderProps()] = start

                    // after we have visited the children increment start number
                    val orderedList = parent
                    orderedList.startNumber = orderedList.startNumber + 1
                } else {
                    CoreProps.LIST_ITEM_TYPE[visitor.renderProps()] = CoreProps.ListItemType.BULLET
                    CoreProps.BULLET_LIST_ITEM_LEVEL[visitor.renderProps()] =
                        listLevel(listItem)
                }
                visitor.setSpansForNodeOptional(listItem, length)
                if (visitor.hasNext(listItem)) {
                    visitor.ensureNewLine()
                }
            }
        }

        private fun listLevel(node: Node): Int {
            var level = 0
            var parent = node.parent
            while (parent != null) {
                if (parent is ListItem) {
                    level += 1
                }
                parent = parent.parent
            }
            return level
        }

        private fun thematicBreak(builder: MarkwonVisitor.Builder) {
            builder.on(ThematicBreak::class.java) { visitor, thematicBreak ->
                visitor.blockStart(thematicBreak)
                val length = visitor.length()

                visitor.builder().append('\u00a0')
                visitor.setSpansForNodeOptional(thematicBreak, length)
                visitor.blockEnd(thematicBreak)
            }
        }

        private fun heading(builder: MarkwonVisitor.Builder) {
            builder.on(Heading::class.java) { visitor, heading ->
                visitor.blockStart(heading)
                val length = visitor.length()
                visitor.visitChildren(heading)
                CoreProps.HEADING_LEVEL[visitor.renderProps()] = heading.level
                visitor.setSpansForNodeOptional(heading, length)
                visitor.blockEnd(heading)
            }
        }

        private fun softLineBreak(builder: MarkwonVisitor.Builder) {
            builder.on(
                SoftLineBreak::class.java
            ) { visitor, softLineBreak -> visitor.builder().append(' ') }
        }

        private fun hardLineBreak(builder: MarkwonVisitor.Builder) {
            builder.on(
                HardLineBreak::class.java
            ) { visitor, hardLineBreak -> visitor.ensureNewLine() }
        }

        private fun paragraph(builder: MarkwonVisitor.Builder) {
            builder.on(Paragraph::class.java) { visitor, paragraph ->
                val inTightList =
                    isInTightList(paragraph)
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

        private fun link(builder: MarkwonVisitor.Builder) {
            builder.on(Link::class.java) { visitor, link ->
                val length = visitor.length()
                visitor.visitChildren(link)
                val destination = link.destination
                CoreProps.LINK_DESTINATION[visitor.renderProps()] = destination
                visitor.setSpansForNodeOptional(link, length)
            }
        }
    }
}