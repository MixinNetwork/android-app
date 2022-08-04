package one.mixin.android.ui.player.internal

import android.support.v4.media.MediaMetadataCompat
import com.github.difflib.DiffUtils
import com.github.difflib.patch.AbstractDelta
import com.github.difflib.patch.DeltaType
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import timber.log.Timber

internal class MusicPlayerUpdater(private val player: ExoPlayer) {
    private var mediaSource = ConcatenatingMediaSource()
    private val cacheDataSourceFactory = CacheDataSourceFactory(MixinApplication.appContext)
    private val dataSourceFactory: DefaultDataSource.Factory by lazy {
        DefaultDataSource.Factory(MixinApplication.appContext) {
            DefaultDataSource(
                MixinApplication.appContext,
                Util.getUserAgent(MixinApplication.appContext, BuildConfig.APPLICATION_ID),
                true
            )
        }
    }

    suspend fun update(incoming: List<MediaMetadataCompat>) {
        val find = incoming.find { it.id == player.currentMediaItem?.mediaId }
        if (find == null) {
            Timber.d("@@@ update use incoming")
            val mediaSource = incoming.toMediaSource(dataSourceFactory, cacheDataSourceFactory)
            this.mediaSource = mediaSource
            player.setMediaSource(mediaSource)
            player.prepare()
            return
        }

        val oldItems = player.currentMediaItems()
        val newItems = incoming.toMediaItems()

        val patch = withContext(Dispatchers.Default) {
            DiffUtils.diff(oldItems, newItems)
        }

        var changed = false
        patch.deltas.forEach { delta ->
            Timber.d("@@@ delta type: ${delta.type}, target size: ${delta.target.size()}, source size: ${delta.source.size()}")
            when (delta.type) {
                DeltaType.INSERT -> delta.insert()
                DeltaType.DELETE -> {
                    delta.delete()
                    changed = true
                }
                DeltaType.CHANGE -> {
                    delta.delete()
                    delta.insert()
                    changed = true
                }
                else -> {}
            }
        }
        if (changed) {
            player.setMediaSource(mediaSource)
            player.prepare()
        }
    }

    fun indexOfMediaItem(mediaId: String): Int {
        for (i in 0 until mediaSource.size) {
            val id = mediaSource.getMediaSource(i).mediaItem.mediaId
            if (mediaId == id) {
                return i
            }
        }
        return -1
    }

    private fun AbstractDelta<MediaItem>.delete() {
        mediaSource.removeMediaSourceRange(target.position, target.position + source.lines.size)
    }

    private fun AbstractDelta<MediaItem>.insert() {
        mediaSource.addMediaSources(target.position, target.lines.toMediaSources(dataSourceFactory, cacheDataSourceFactory))
    }
}
