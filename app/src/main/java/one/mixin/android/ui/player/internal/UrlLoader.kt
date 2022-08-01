package one.mixin.android.ui.player.internal

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.ui.player.MusicService.Companion.MUSIC_PLAYLIST

class UrlLoader : MusicMetaLoader() {
    private val observers = mutableSetOf<UrlObserver>()
    private var mediaList: MutableList<MediaMetadataCompat>? = null

    suspend fun load(urls: Array<String>?): List<MediaMetadataCompat> {
        if (urls.isNullOrEmpty()) return emptyList()

        val mediaMetadataCompatList = mutableListOf<MediaMetadataCompat>()
        withContext(Dispatchers.IO) {
            urls.forEach { url ->
                val musicMeta = retrieveMetadata(url, url, 1000) ?: return@forEach
                mediaMetadataCompatList.add(
                    MediaMetadataCompat.Builder()
                        .from(url, musicMeta)
                        .build()
                )
            }
            mediaMetadataCompatList.forEach { it.description.extras?.putAll(it.bundle) }
        }
        mediaList = mediaMetadataCompatList

        observers.forEach { it.onUpdate(mediaMetadataCompatList) }

        return mediaMetadataCompatList
    }

    fun addObserver(observer: UrlObserver) {
        observers.add(observer)
        mediaList?.let { observer.onUpdate(it) }
    }

    fun removeObserver(observer: UrlObserver) {
        observers.remove(observer)
    }

    fun clear() {
        mediaList?.clear()
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

    fun interface UrlObserver {
        fun onUpdate(mediaList: List<MediaMetadataCompat>)
    }
}

val urlLoader = UrlLoader()
