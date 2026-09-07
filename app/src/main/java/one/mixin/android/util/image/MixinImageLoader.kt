package one.mixin.android.util.image

import android.os.Build
import android.os.Build.VERSION.SDK_INT
import androidx.annotation.RequiresApi
import coil3.PlatformContext
import coil3.annotation.ExperimentalCoilApi
import coil3.ImageLoader as CoilImageLoader
import coil3.disk.DiskCache
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import coil3.network.CacheStrategy
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.cachecontrol.CacheControlCacheStrategy
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import coil3.request.SuccessResult
import coil3.svg.SvgDecoder
import coil3.util.DebugLogger
import coil3.video.VideoFrameDecoder
import okhttp3.Call
import okhttp3.OkHttpClient
import one.mixin.android.BuildConfig
import one.mixin.android.di.AppModule.API_UA
import java.io.File
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoilApi::class)
@RequiresApi(Build.VERSION_CODES.P)
internal fun newMixinImageLoader(
    context: PlatformContext,
    callFactory: () -> Call.Factory = ::newImageCallFactory,
    diskCache: DiskCache? = null,
): CoilImageLoader {
    return CoilImageLoader.Builder(context)
        .components {
            add(
                OkHttpNetworkFetcherFactory(
                    callFactory = callFactory,
                    cacheStrategy = ::PersistentImageCacheStrategy,
                ),
            )
            if (SDK_INT >= Build.VERSION_CODES.P) {
                add(AnimatedImageDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
            add(SvgDecoder.Factory())
            add(VideoFrameDecoder.Factory())
        }.apply {
            if (diskCache != null) {
                diskCache(diskCache)
            }
            if (BuildConfig.DEBUG) {
                logger(DebugLogger())
            }
        }
        .build()
}

@OptIn(ExperimentalCoilApi::class, ExperimentalTime::class)
private class PersistentImageCacheStrategy : CacheStrategy {
    private val cacheControlStrategy = CacheControlCacheStrategy()

    override suspend fun read(
        cacheResponse: NetworkResponse,
        networkRequest: NetworkRequest,
        options: Options,
    ): CacheStrategy.ReadResult = CacheStrategy.DEFAULT.read(cacheResponse, networkRequest, options)

    override suspend fun write(
        cacheResponse: NetworkResponse?,
        networkRequest: NetworkRequest,
        networkResponse: NetworkResponse,
        options: Options,
    ): CacheStrategy.WriteResult =
        cacheControlStrategy.write(cacheResponse, networkRequest, networkResponse, options)
}

private fun newImageCallFactory(): Call.Factory =
    OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request =
                original.newBuilder()
                    .header("User-Agent", API_UA)
                    .method(original.method, original.body)
                    .build()
            chain.proceed(request)
        }
        .build()

@OptIn(ExperimentalCoilApi::class)
internal inline fun <T> CoilImageLoader.withDiskCacheFile(
    result: SuccessResult,
    block: (File) -> T,
): T? {
    val cacheKey = result.diskCacheKey ?: return null
    val cache = diskCache ?: return null
    return cache.openSnapshot(cacheKey)?.use { snapshot ->
        block(snapshot.data.toFile())
    }
}
