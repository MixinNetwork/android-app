package one.mixin.android.ui.player.internal

import android.content.Context
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat

class BrowseTree(
    val context: Context,
    musicSourceMap: HashMap<String, MusicSource>,
) {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>()

    init {
        val rootList = mediaIdToChildren[MUSIC_BROWSABLE_ROOT] ?: mutableListOf()
        mediaIdToChildren[MUSIC_BROWSABLE_ROOT] = rootList

        musicSourceMap.forEach { entry ->
            addMusicSource(entry.value)
        }
    }

    fun addMusicSource(musicSource: MusicSource) {
        musicSource.forEach { mediaItem ->
            val albumMediaId = mediaItem.album ?: MUSIC_UNKNOWN_ROOT
            val albumChildren = mediaIdToChildren[albumMediaId] ?: buildAlbumRoot(mediaItem)
            val exists = albumChildren.find { it.description.mediaId == mediaItem.id }
            if (exists == null) {
                albumChildren += mediaItem
            }
        }
    }

    fun addPlaylist(musicSource: MusicSource) {
        mediaIdToChildren[MUSIC_PLAYLIST]?.clear()
        addMusicSource(musicSource)
    }

    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMediaId = mediaItem.album ?: MUSIC_UNKNOWN_ROOT
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = albumMediaId
            title = mediaItem.album
            flag = MediaItem.FLAG_BROWSABLE
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