package one.mixin.android.ui.player.internal

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

internal val Player.currentMediaItems: List<MediaItem> get() =
    List(mediaItemCount, ::getMediaItemAt)
