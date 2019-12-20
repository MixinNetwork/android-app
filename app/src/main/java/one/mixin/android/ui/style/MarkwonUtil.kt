package one.mixin.android.ui.style

import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.syntax.Prism4jThemeDarkula
import io.noties.markwon.syntax.Prism4jThemeDefault
import io.noties.markwon.syntax.SyntaxHighlightPlugin
import io.noties.prism4j.Prism4j
import one.mixin.android.MixinApplication
import one.mixin.android.util.language.LanguageGrammerLocator

class MarkwonUtil {
    companion object {
        private var markDown: Markwon? = null
        fun getSingle(
            isNightMode: Boolean
        ): Markwon {
            val context = MixinApplication.appContext
            val prism4j = Prism4j(LanguageGrammerLocator())
            val prism4jTheme = if (isNightMode) {
                Prism4jThemeDarkula.create()
            } else Prism4jThemeDefault.create()
            if (markDown == null) {
                markDown = Markwon.builder(context)
                    .usePlugin(object : AbstractMarkwonPlugin() {
                        override fun configureTheme(builder: MarkwonTheme.Builder) {
                            builder.headingBreakHeight(0)
                        }
                    })
                    .usePlugin(StrikethroughPlugin.create())
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
                    .usePlugin(SyntaxHighlightPlugin.create(prism4j, prism4jTheme))
                    .build()
            }
            return markDown!!
        }

        private var miniMarkDown: Markwon? = null
        fun getMiniSingle(): Markwon {
            val context = MixinApplication.appContext
            if (miniMarkDown == null) {
                miniMarkDown = Markwon.builder(context)
                    .usePlugin(object : AbstractMarkwonPlugin() {
                        override fun configureTheme(builder: MarkwonTheme.Builder) {
                            builder.headingBreakHeight(0)
                            builder.headingTextSizeMultipliers(floatArrayOf(1.3F, 1.2F, 1.1F, .9F, .8F, .7F))
                        }
                    })
                    .usePlugin(StrikethroughPlugin.create())
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
                    .build()
            }
            return miniMarkDown!!
        }
    }
}
