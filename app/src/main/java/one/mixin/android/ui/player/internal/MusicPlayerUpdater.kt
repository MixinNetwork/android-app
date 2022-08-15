package one.mixin.android.ui.player.internal

import android.support.v4.media.MediaMetadataCompat
import com.github.difflib.DiffUtils
import com.github.difflib.patch.AbstractDelta
import com.github.difflib.patch.DeltaType
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class MusicPlayerUpdater(private val player: ExoPlayer) {

    suspend fun update(incoming: List<MediaMetadataCompat>) {
        val find = incoming.find { it.id == player.currentMediaItem?.mediaId }
        if (find == null) {
            val mediaItems = incoming.toMediaItems()
            player.setMediaItems(mediaItems)
            player.prepare()
            return
        }

        val oldItems = player.currentMediaItems
        val newItems = incoming.toMediaItems()

        val patch = withContext(Dispatchers.Default) {
            DiffUtils.diff(oldItems, newItems)
        }

        patch.deltas.forEach { delta ->
            when (delta.type) {
                DeltaType.INSERT -> delta.insert()
                DeltaType.DELETE -> delta.delete()
                DeltaType.CHANGE -> {
                    delta.delete()
                    delta.insert()
                }
                else -> {}
            }
        }
    }

    private fun AbstractDelta<MediaItem>.delete() {
        player.removeMediaItems(target.position, target.position + source.lines.size)
    }

    private fun AbstractDelta<MediaItem>.insert() {
        player.addMediaItems(target.position, target.lines)
    }
}
