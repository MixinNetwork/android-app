package one.mixin.android.util.markdown

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolverDef
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonSpansFactory
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin.GlideStore
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.isActivityNotDestroyed
import one.mixin.android.extension.isMixinUrl
import one.mixin.android.extension.isNightMode
import one.mixin.android.extension.postOptimize
import one.mixin.android.util.markdown.table.TableEntryPlugin
import one.mixin.android.widget.markdown.SimplePlugin
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.SoftLineBreak

class MarkwonUtil {
    companion object {

        val simpleMarkwon by lazy {
            Markwon.builderNoCore(MixinApplication.appContext).usePlugin(SimplePlugin()).build()
        }

        fun getMarkwon(
            context: Activity,
            mixinLinkResolver: (String) -> Unit,
            linkResolver: (String) -> Unit
        ): Markwon {
            val isNightMode = context.isNightMode()
            val prism4j = Prism4j(LanguageGrammerLocator())
            val prism4jTheme = if (isNightMode) {
                Prism4jThemeDarkula.create()
            } else Prism4jThemeDefault.create()
            return Markwon.builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(HtmlPlugin.create())
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                .usePlugin(TableEntryPlugin.create(context))
                .usePlugin(createGlidePlugin(context))
                .usePlugin(
                    object : AbstractMarkwonPlugin() {
                        override fun configureTheme(builder: MarkwonTheme.Builder) {
                            builder.headingBreakHeight(0)
                                .headingTextSizeMultipliers(
                                    floatArrayOf(
                                        1.32F,
                                        1.24F,
                                        1.18F,
                                        1.1F,
                                        1.0F,
                                        0.9F
                                    )
                                )
                        }

                        override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                            val spansFactory = builder.getFactory(Link::class.java)
                            if (spansFactory != null) {
                                builder.setFactory(Link::class.java) { configuration, props ->
                                    arrayOf(
                                        RemoveUnderlineSpan(),
                                        spansFactory.getSpans(
                                            configuration,
                                            props
                                        )
                                    )
                                }
                            }
                        }

                        override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                            builder.on(FencedCodeBlock::class.java) { visitor: MarkwonVisitor, fencedCodeBlock: FencedCodeBlock ->
                                val code = visitor.configuration()
                                    .syntaxHighlight()
                                    .highlight(
                                        fencedCodeBlock.info,
                                        fencedCodeBlock.literal.trim { it <= ' ' }
                                    )
                                visitor.builder().append(code)
                            }
                            builder.on(SoftLineBreak::class.java) { visitor: MarkwonVisitor, _: SoftLineBreak ->
                                visitor.forceNewLine()
                            }
                        }

                        override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                            builder.linkResolver(
                                object : LinkResolverDef() {
                                    override fun resolve(view: View, link: String) {
                                        if (link.isMixinUrl()) {
                                            mixinLinkResolver.invoke(link)
                                        } else {
                                            linkResolver.invoke(link)
                                        }
                                    }
                                }
                            )
                        }
                    }
                ).build()
        }

        fun getMiniMarkwon(context: Activity): Markwon {
            val isNightMode = context.isNightMode()
            val prism4j = Prism4j(LanguageGrammerLocator())
            val prism4jTheme = if (isNightMode) {
                Prism4jThemeDarkula.create()
            } else Prism4jThemeDefault.create()
            return Markwon.builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(HtmlPlugin.create())
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                .usePlugin(TablePlugin.create(context))
                .usePlugin(createGlidePlugin(context))
                .usePlugin(
                    object : AbstractMarkwonPlugin() {
                        override fun configureTheme(builder: MarkwonTheme.Builder) {
                            builder.headingBreakHeight(0)
                                .codeBlockBackgroundColor(context.colorFromAttribute(R.attr.bg_block))
                                .codeBackgroundColor(context.colorFromAttribute(R.attr.bg_block))
                                .headingTextSizeMultipliers(
                                    floatArrayOf(
                                        1.32F,
                                        1.24F,
                                        1.18F,
                                        1.1F,
                                        1.0F,
                                        0.9F
                                    )
                                )
                        }

                        override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                            builder.linkResolver { _, _ -> }
                        }

                        override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                            val spansFactory = builder.getFactory(Link::class.java)
                            if (spansFactory != null) {
                                builder.setFactory(Link::class.java) { configuration, props ->
                                    arrayOf(
                                        RemoveUnderlineSpan(),
                                        spansFactory.getSpans(
                                            configuration,
                                            props
                                        )
                                    )
                                }
                            }
                        }

                        override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                            builder.on(FencedCodeBlock::class.java) { visitor: MarkwonVisitor, fencedCodeBlock: FencedCodeBlock ->
                                val code = visitor.configuration()
                                    .syntaxHighlight()
                                    .highlight(
                                        fencedCodeBlock.info,
                                        fencedCodeBlock.literal.trim { it <= ' ' }
                                    )
                                visitor.builder().append(code)
                            }
                            builder.on(SoftLineBreak::class.java) { visitor: MarkwonVisitor, _: SoftLineBreak ->
                                visitor.forceNewLine()
                            }
                        }
                    }
                )
                .build()
        }

        private val markwon by lazy {
            Markwon.create(MixinApplication.appContext)
        }

        fun parseContent(content: String?): String {
            content
                ?: return MixinApplication.appContext.getString(R.string.content_post)
            return markwon.toMarkdown(content.postOptimize()).toString()
        }

        private fun createGlidePlugin(context: Context): GlideImagesPlugin =
            GlideImagesPlugin.create(
                object : GlideStore {
                    override fun cancel(target: com.bumptech.glide.request.target.Target<*>) {
                        if (context.isActivityNotDestroyed()) {
                            Glide.with(context).clear(target)
                        }
                    }

                    override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
                        return Glide.with(context).load(drawable.destination)
                    }
                }
            )
    }
}
