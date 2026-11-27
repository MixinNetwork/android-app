package one.mixin.android.ui.media.pager

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.util.video.VideoPlayerControls
import java.io.File

internal class VideoFramePreviewLoader {
    private val io = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cache = LruCache<Long, Bitmap>(24)
    private var source: String? = null
    private var pending: Job? = null
    // All retriever access, including release after a cancelled decode, runs on io.
    private var retriever: MediaMetadataRetriever? = null

    fun setVideoSource(path: String?) {
        val local = when {
            path == null -> null
            path.startsWith("/") -> path
            path.startsWith("file://") -> Uri.parse(path).path
            else -> null
        }
        if (source == local) return
        pending?.cancel()
        cache.evictAll()
        source = local
        CoroutineScope(io).launch {
            retriever?.release()
            retriever = null
        }
    }

    fun loadFrame(positionMs: Long, onResult: (Bitmap?) -> Unit) {
        pending?.cancel()
        val path = source ?: return onResult(null)
        val bucket = VideoPlayerControls.frameBucket(positionMs)
        cache.get(bucket)?.let { return onResult(it) }
        pending = scope.launch {
            val frame = withContext(io) {
                try {
                    if (retriever == null) {
                        if (!File(path).isFile) return@withContext null
                        retriever = MediaMetadataRetriever()
                        retriever!!.setDataSource(path)
                    }
                    val bitmap = retriever!!.getFrameAtTime(bucket * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    if (bitmap == null || maxOf(bitmap.width, bitmap.height) <= 240) return@withContext bitmap
                    val scale = 240f / maxOf(bitmap.width, bitmap.height)
                    Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true).also { bitmap.recycle() }
                } catch (_: RuntimeException) {
                    retriever?.release()
                    retriever = null
                    null
                }
            }
            if (frame != null) cache.put(bucket, frame)
            onResult(frame)
        }
    }

    fun release() {
        scope.cancel()
        setVideoSource(null)
    }
}
