package one.mixin.android.ui.player.internal

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import one.mixin.android.MixinApplication
import one.mixin.android.R

class PlaylistSource(
    private val playlist: Array<String>
) : AbstractMusicSource() {
    private var catalog: List<MediaMetadataCompat> = emptyList()

    init {
        state = STATE_INITIALIZING
    }

    override suspend fun load() {
        val mediaMetadataCompats = mutableListOf<MediaMetadataCompat>()
        playlist.forEach { p ->
            mediaMetadataCompats.add(
                MediaMetadataCompat.Builder()
                    .from(p)
                    .build()
            )
        }
        mediaMetadataCompats.forEach { it.description.extras?.putAll(it.bundle) }
        if (!mediaMetadataCompats.isNullOrEmpty()) {
            catalog = mediaMetadataCompats
            state = STATE_INITIALIZED
        } else {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()

    fun MediaMetadataCompat.Builder.from(url: String): MediaMetadataCompat.Builder {
        id = url
        val unknownString = MixinApplication.appContext.getString(R.string.unknown)
        title = unknownString
        artist = unknownString
        album = MUSIC_PLAYLIST
        mediaUri = url

        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE

        displayTitle = unknownString
        displaySubtitle = unknownString

        albumArtUri = "android.resource://one.mixin.messenger/drawable/ic_avatar_place_holder"
        downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED

        return this
    }
}