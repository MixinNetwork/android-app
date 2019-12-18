package one.mixin.android.ui.style

import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import io.noties.markwon.Markwon
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.glide.GlideImagesPlugin
import one.mixin.android.MixinApplication

class MarkDown {
    companion object {
        private var markDown: Markwon? = null
        fun getSingle(): Markwon {
            val context = MixinApplication.appContext
            if (markDown == null) {
                markDown = Markwon.builder(context)
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
            return markDown!!
        }
    }
}