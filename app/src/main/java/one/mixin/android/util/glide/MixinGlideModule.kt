package one.mixin.android.util.glide

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import one.mixin.android.widget.RLottieDrawable
import java.io.InputStream

@GlideModule
class MixinGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry
            .register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
            .register(RLottie::class.java, RLottieDrawable::class.java, LottieDrawableTranscoder())
            .append(InputStream::class.java, SVG::class.java, SvgDecoder())
            .append(Any::class.java, RLottie::class.java, LottieDecoder())
    }

    override fun isManifestParsingEnabled(): Boolean = false
}
