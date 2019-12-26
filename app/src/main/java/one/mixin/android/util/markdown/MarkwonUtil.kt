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
        private var markDownNight: Boolean = false
        private var markDown: Markwon? = null
        private var linkResolver: ((String) -> Unit)? = null
        fun getSingle(
            context: Context,
            linkResolver: (String) -> Unit
        ): Markwon {
            val isNightMode = context.isNightMode()
            this.linkResolver = linkResolver
            if (markDown == null || markDownNight != isNightMode) {
                val prism4j = Prism4j(LanguageGrammerLocator())
                val prism4jTheme = Prism4jThemeDefault.create()
                markDown = Markwon.builder(context)
                    .usePlugin(CorePlugin.create())
                    .usePlugin(StrikethroughPlugin.create())
                    .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                    .usePlugin(TableEntryPlugin.create(context))
                    .usePlugin(GlideImagesPlugin.create(context))
                    .usePlugin(GlideImagesPlugin.create(Glide.with(context)))
                    .usePlugin(GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {
                        override fun cancel(target: com.bumptech.glide.request.target.Target<*>) {
                            Glide.with(context).clear(target)
                        }

                        override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
                            return Glide.with(context).load(drawable.destination)
                        }
                    }))

                    .usePlugin(object : AbstractMarkwonPlugin() {
                        override fun configureTheme(builder: MarkwonTheme.Builder) {
                            builder.headingBreakHeight(0)
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
                                        this@Companion.linkResolver?.invoke(link)
                                    } else {
                                        super.resolve(view, link)
                                    }
                                }
                            })
                        }
                    }).build()
            }
            return markDown!!
        }

        private var miniMarkDownNight: Boolean = false
        private var miniMarkDown: Markwon? = null
        fun getMiniSingle(context: Context): Markwon {
            val isNightMode = context.isNightMode()
            if (miniMarkDown == null || miniMarkDownNight != isNightMode) {
                val prism4j = Prism4j(LanguageGrammerLocator())
                val prism4jTheme = if (isNightMode) {
                    Prism4jThemeDarkula.create()
                } else Prism4jThemeDefault.create()
                miniMarkDown = Markwon.builder(context)
                    .usePlugin(CorePlugin.create())
                    .usePlugin(StrikethroughPlugin.create())
                    .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                    .usePlugin(TablePlugin.create(getTheme()))
                    .usePlugin(GlideImagesPlugin.create(context))
                    .usePlugin(GlideImagesPlugin.create(Glide.with(context)))
                    .usePlugin(GlideImagesPlugin.create(object : GlideImagesPlugin.GlideStore {
                        override fun cancel(target: com.bumptech.glide.request.target.Target<*>) {
                            Glide.with(context).clear(target)
                        }

                        override fun load(drawable: AsyncDrawable): RequestBuilder<Drawable> {
                            return Glide.with(context).load(drawable.destination)
                        }
                    }))
                    .usePlugin(object : AbstractMarkwonPlugin() {
                        override fun configureTheme(builder: MarkwonTheme.Builder) {
                            builder.headingBreakHeight(0)
                                .codeBlockBackgroundColor(context.colorFromAttribute(R.attr.bg_block))
                                .codeBackgroundColor(context.colorFromAttribute(R.attr.bg_block))
                                .headingTextSizeMultipliers(
                                    floatArrayOf(
                                        1.3F,
                                        1.2F,
                                        1.1F,
                                        .9F,
                                        .8F,
                                        .7F
                                    )
                                )
                        }

                        override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                            builder.linkResolver { _, _ -> }
                        }
                    })
                    .build()
            }
            return miniMarkDown!!
        }

        private fun getTheme(): TableTheme {
            return TableTheme.Builder()
                .tableBorderWidth(1)
                .tableCellPadding(1)
                .build()
        }
    }
}
