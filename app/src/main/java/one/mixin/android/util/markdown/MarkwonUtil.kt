package one.mixin.android.util.markdown

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.bumptech.glide.Glide
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.LinkResolverDef
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.MarkwonVisitor
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tables.TableTheme
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.recycler.table.TableEntryPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.isNightMode
import one.mixin.android.ui.url.isMixinUrl
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.SoftLineBreak
import org.jetbrains.anko.dip

class MarkwonUtil {
    companion object {

        private val placeholder = ColorDrawable(Color.parseColor("#efefef"))
            .apply {
                val size = MixinApplication.appContext.dip(48)
                setBounds(0, 0, size, size)
            }

        fun getMarkwon(
            context: Context,
            mixinLinkResolver: (String) -> Unit,
            linkResolver: (String) -> Unit
        ): Markwon {
            val requestManager = Glide.with(context)
            val isNightMode = context.isNightMode()
            val prism4j = Prism4j(LanguageGrammerLocator())
            val prism4jTheme = if (isNightMode) {
                Prism4jThemeDarkula.create()
            } else Prism4jThemeDefault.create()
            return Markwon.builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                .usePlugin(TableEntryPlugin.create(context))
                .usePlugin(ImagesPlugin.create().placeholderProvider { placeholder })
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder.headingBreakHeight(0)
                            .headingTextSizeMultipliers(floatArrayOf(1.32F, 1.24F, 1.18F, 1.1F, 1.0F, 0.9F))
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
                        builder.linkResolver(object : LinkResolverDef() {
                            override fun resolve(view: View, link: String) {
                                if (isMixinUrl(link)) {
                                    mixinLinkResolver.invoke(link)
                                } else {
                                    linkResolver.invoke(link)
                                }
                            }
                        })
                    }
                }).build()
        }

        fun getMiniMarkwon(context: Context): Markwon {
            val isNightMode = context.isNightMode()
            val requestManager = Glide.with(context)

            val prism4j = Prism4j(LanguageGrammerLocator())
            val prism4jTheme = if (isNightMode) {
                Prism4jThemeDarkula.create()
            } else Prism4jThemeDefault.create()
            return Markwon.builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(TaskListPlugin.create(context))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                .usePlugin(TablePlugin.create(getTheme()))
                .usePlugin(ImagesPlugin.create().placeholderProvider { placeholder })
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder.headingBreakHeight(0)
                            .codeBlockBackgroundColor(context.colorFromAttribute(R.attr.bg_block))
                            .codeBackgroundColor(context.colorFromAttribute(R.attr.bg_block))
                            .headingTextSizeMultipliers(floatArrayOf(1.32F, 1.24F, 1.18F, 1.1F, 1.0F, 0.9F))
                    }

                    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                        builder.linkResolver { _, _ -> }
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
                })
                .build()
        }

        private fun getTheme(): TableTheme {
            return TableTheme.Builder()
                .tableBorderWidth(1)
                .tableCellPadding(1)
                .build()
        }
    }
}
