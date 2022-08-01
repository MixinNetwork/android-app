package one.mixin.android.ui.player.internal

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player

internal fun Player.currentMediaItems(): List<MediaItem> {
    return List(mediaItemCount, ::getMediaItemAt)
}