package one.mixin.android.util.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder
import one.mixin.android.widget.RLottieDrawable

class LottieDrawableTranscoder : ResourceTranscoder<RLottie, RLottieDrawable> {
    override fun transcode(toTranscode: Resource<RLottie>, options: Options): Resource<RLottieDrawable> {
        val rLottie = toTranscode.get()
        val lottieDrawable = RLottieDrawable(
            rLottie.file,
            rLottie.w,
            rLottie.h,
            false,
            false,
        )
        return SimpleResource(lottieDrawable)
    }
}
