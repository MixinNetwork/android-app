package one.mixin.android.util.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceDecoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.SimpleResource
import java.io.File
import java.io.IOException

class LottieDecoder : ResourceDecoder<Any, RLottie> {

    override fun handles(source: Any, options: Options): Boolean = true

    @Throws(IOException::class)
    override fun decode(
        source: Any,
        width: Int,
        height: Int,
        options: Options
    ): Resource<RLottie> {
        return try {
            SimpleResource(RLottie(source as File, width, height))
        } catch (ex: Exception) {
            throw IOException("Cannot load lottie from source", ex)
        }
    }
}
