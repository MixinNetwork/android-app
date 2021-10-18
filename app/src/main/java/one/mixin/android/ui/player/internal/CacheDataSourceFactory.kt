package one.mixin.android.ui.player.internal

import android.content.Context
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.FileDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSink
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import one.mixin.android.MixinApplication
import java.io.File

class CacheDataSourceFactory(
    context: Context,
    private val maxFileSize: Long = (50 * 1024 * 1024).toLong(),
) : DataSource.Factory {
    private val defaultDataSourceFactory = DefaultDataSourceFactory(
        context,
        DefaultHttpDataSource.Factory()
    )

    override fun createDataSource(): DataSource {

        return CacheDataSource(
            simpleCache,
            defaultDataSourceFactory.createDataSource(),
            FileDataSource(),
            CacheDataSink(simpleCache, maxFileSize),
            CacheDataSource.FLAG_BLOCK_ON_CACHE or CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR,
            null,
        )
    }
}

val simpleCache = SimpleCache(
    File(MixinApplication.appContext.cacheDir, "exo-music"),
    LeastRecentlyUsedCacheEvictor((100 * 1024 * 1024).toLong()),
    ExoDatabaseProvider(MixinApplication.appContext)
)
