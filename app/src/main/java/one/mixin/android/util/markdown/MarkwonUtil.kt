package one.mixin.android.util.markdown

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
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
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.recycler.table.TableEntryPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import one.mixin.android.R
import one.mixin.android.extension.colorFromAttribute
import one.mixin.android.extension.isNightMode
import one.mixin.android.ui.url.isMixinUrl
import org.commonmark.node.FencedCodeBlock

class MarkwonUtil {
    companion object {
        fun getMarkwon(
            context: Context,
            linkResolver: (String) -> Unit
        ): Markwon {
            val requestManager = Glide.with(context)
            val prism4j = Prism4j(LanguageGrammerLocator())
            val prism4jTheme = Prism4jThemeDefault.create()
            return Markwon.builder(context)
                .usePlugin(CorePlugin.create())
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                .usePlugin(TableEntryPlugin.create(context))
                .usePlugin(GlideImagesPlugin.create(context))
                .usePlugin(GlideImagesPlugin.create(Glide.with(context)))
                .usePlugin(GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {
                    override fun cancel(target: com.bumptech.glide.request.target.Target<*>) {
                        requestManager.clear(target)
                    }

                    override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
                        return requestManager.load(drawable.destination)
                    }
                }))
                .usePlugin(object : AbstractMarkwonPlugin() {
                    override fun configureTheme(builder: MarkwonTheme.Builder) {
                        builder.headingBreakHeight(0)
                            .headingTextSizeMultipliers(floatArrayOf(1.32F, 1.24F, 1.18F, 1.1F, 1.0F, 0.9F))
                    }

                    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                        builder.on(
                            FencedCodeBlock::class.java
                        ) { visitor: MarkwonVisitor, fencedCodeBlock: FencedCodeBlock ->
                            val code = visitor.configuration()
                                .syntaxHighlight()
                                .highlight(
                                    fencedCodeBlock.info,
                                    fencedCodeBlock.literal.trim { it <= ' ' }
                                )
                            visitor.builder().append(code)
                        }
                    }

                    override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                        builder.linkResolver(object : LinkResolverDef() {
                            override fun resolve(view: View, link: String) {
                                if (isMixinUrl(link)) {
                                    linkResolver.invoke(link)
                                } else {
                                    super.resolve(view, link)
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
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                .usePlugin(TablePlugin.create(getTheme()))
                .usePlugin(GlideImagesPlugin.create(context))
                .usePlugin(GlideImagesPlugin.create(Glide.with(context)))
                .usePlugin(GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {
                    override fun cancel(target: com.bumptech.glide.request.target.Target<*>) {
                        requestManager.clear(target)
                    }

                    override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
                        return requestManager.load(drawable.destination)
                    }
                }))
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
