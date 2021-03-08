package one.mixin.android.ui.player.internal

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat

class PlaylistLoader(private val playlist: Array<String>) : MusicLoader() {

    override suspend fun load(): List<MediaMetadataCompat> {
        val mediaMetadataCompats = mutableListOf<MediaMetadataCompat>()
        playlist.forEach { url ->
            val musicMeta = retrieveMetadata(url, url, 1000) ?: return@forEach
            mediaMetadataCompats.add(
                MediaMetadataCompat.Builder()
                    .from(url, musicMeta)
                    .build()
            )
        }
        mediaMetadataCompats.forEach { it.description.extras?.putAll(it.bundle) }
        return mediaMetadataCompats
    }

    private fun MediaMetadataCompat.Builder.from(url: String, musicMeta: MusicMeta): MediaMetadataCompat.Builder {
        id = url
        var titleString = musicMeta.title
        if (titleString == null) {
            titleString = try {
                url.substring(url.lastIndexOf('/') + 1)
            } catch (e: IndexOutOfBoundsException) {
                unknownString
            }
        }
        title = titleString
        artist = musicMeta.artist ?: unknownString
        album = MUSIC_PLAYLIST
        mediaUri = url
        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        albumArtUri = musicMeta.albumArt
        displayTitle = titleString
        displaySubtitle = musicMeta.artist ?: unknownString
        displayIconUri = musicMeta.albumArt
        downloadStatus = MediaDescriptionCompat.STATUS_DOWNLOADED
        return this
    }
}
