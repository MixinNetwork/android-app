package one.mixin.android.ui.player.internal

import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player

internal val Player.currentMediaItems: List<MediaItem> get() =
    List(mediaItemCount, ::getMediaItemAt)
