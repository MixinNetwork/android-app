package one.mixin.android.ui.player.internal

import android.support.v4.media.MediaMetadataCompat

interface MusicLoader {

    suspend fun load(): List<MediaMetadataCompat>
}
