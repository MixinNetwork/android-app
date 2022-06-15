package one.mixin.android.ui.player.internal

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import kotlinx.coroutines.withContext
import one.mixin.android.util.SINGLE_THREAD

class MusicTree {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>()

    init {
        val rootList = mediaIdToChildren[MUSIC_BROWSABLE_ROOT] ?: mutableListOf()
        mediaIdToChildren[MUSIC_BROWSABLE_ROOT] = rootList
    }

    suspend fun setItems(mediaItems: List<MediaMetadataCompat>, clear: Boolean = false) = withContext(SINGLE_THREAD) {
        if (clear) {
            mediaIdToChildren[mediaItems[0].album]?.clear()
        }
        mediaItems.forEach { mediaItem ->
            setItem(mediaItem)
        }
    }

    suspend fun updatePlaylist(mediaItems: List<MediaMetadataCompat>) = withContext(SINGLE_THREAD) {
        mediaIdToChildren[MUSIC_PLAYLIST]?.clear()
        setItems(mediaItems)
    }

    private fun setItem(mediaItem: MediaMetadataCompat) {
        val albumMediaId = mediaItem.album ?: MUSIC_UNKNOWN_ROOT
        val albumChildren = mediaIdToChildren[albumMediaId] ?: buildAlbumRoot(mediaItem)
        val index = albumChildren.indexOfFirst { it.description.mediaId == mediaItem.id }
        if (index == -1) {
            albumChildren += mediaItem
        } else {
            albumChildren[index] = mediaItem
        }
    }

    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMediaId = mediaItem.album ?: MUSIC_UNKNOWN_ROOT
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = albumMediaId
            title = mediaItem.album
            flag = MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
        }.build()

        val rootList = mediaIdToChildren[MUSIC_BROWSABLE_ROOT] ?: mutableListOf()
        val existsAlbum = mediaIdToChildren[albumMediaId]
        if (existsAlbum == null) {
            rootList += albumMetadata
            mediaIdToChildren[MUSIC_BROWSABLE_ROOT] = rootList
        }

        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[albumMetadata.id!!] = it
        }
    }
}

const val MUSIC_BROWSABLE_ROOT = "/"
const val MUSIC_UNKNOWN_ROOT = "__UNKNOWN__"
const val MUSIC_PLAYLIST = "__PLAYLIST_"
