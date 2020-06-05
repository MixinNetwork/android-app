package one.mixin.android.util.image

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import one.mixin.android.widget.RLottieDrawable
import java.util.concurrent.Callable

sealed class ImageLoader<T> {
    private val taskCache = HashMap<String, ImageTask<T>>()

    protected fun fromUrl(url: String, cacheKey: String? = url, callable: Callable<ImageResult<T>>): ImageTask<T> {
        return cache(cacheKey, callable)
    }

    private fun cache(cacheKey: String?, callable: Callable<ImageResult<T>>): ImageTask<T> {
        if (cacheKey != null && taskCache.containsKey(cacheKey)) {
            val cachedTask = taskCache[cacheKey]
            if (cachedTask != null) {
                return cachedTask
            }
        }

        val task = ImageTask(callable)
        if (cacheKey != null) {
            task.addListener(object : ImageListener<T> {
                override fun onResult(result: T) {
                    taskCache.remove(cacheKey)
                }
            })
            task.addFailureListener(object : ImageListener<Throwable> {
                override fun onResult(result: Throwable) {
                    taskCache.remove(cacheKey)
                }
            })
            taskCache[cacheKey] = task
        }
        return task
    }
}

object LottieLoader : ImageLoader<RLottieDrawable>() {

    fun fromUrl(
        context: Context,
        url: String,
        cacheKey: String? = url,
        w: Int,
        h: Int,
        precache: Boolean = false,
        limitFps: Boolean = false
    ): ImageTask<RLottieDrawable> {
        return fromUrl(
            url, cacheKey,
            Callable {
                val file = NetworkFetcher.fetchSync(context, url)
                ImageResult(RLottieDrawable(file.value, w, h, precache, limitFps))
            }
        )
    }
}

const val TEMP_JSON_EXTENSION = ".temp.json"
const val JSON_EXTENSION = ".json"

object HeicLoader : ImageLoader<Drawable>() {

    @RequiresApi(Build.VERSION_CODES.P)
    fun fromUrl(context: Context, uri: Uri, cacheKey: String? = uri.toString()): ImageTask<Drawable> {
        return fromUrl(
            uri.toString(), cacheKey,
            Callable {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                return@Callable ImageResult(ImageDecoder.decodeDrawable(source))
            }
        )
    }
}
