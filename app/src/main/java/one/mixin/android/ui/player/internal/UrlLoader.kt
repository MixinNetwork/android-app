@file:UnstableApi

package one.mixin.android.ui.player.internal

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import one.mixin.android.ui.player.MusicService.Companion.MUSIC_PLAYLIST

class UrlLoader : MusicMetaLoader() {
    private val observers = mutableSetOf<UrlObserver>()
    private var mediaList: MutableList<MediaMetadataCompat> = mutableListOf()

    suspend fun load(urls: Array<String>?) {
        if (urls.isNullOrEmpty()) return
        mediaList.clear()

        withContext(Dispatchers.IO) {
            val list = urls.toMutableList()

            loadInternal(list.take(1))

            list.drop(1)
                .chunked(5) { chunk ->
                    loadInternal(chunk)
                }
        }
    }

    private fun loadInternal(chunk: List<String>) {
        val mediaMetadataCompatList = mutableListOf<MediaMetadataCompat>()
        chunk.forEach { url ->
            val musicMeta = retrieveMetadata(url, url, 1000) ?: return@forEach
            val item =
                MediaMetadataCompat.Builder()
                    .from(url, musicMeta)
                    .build()
            item.description.extras?.putAll(item.bundle)
            mediaMetadataCompatList.add(item)
        }
        mediaList += mediaMetadataCompatList
        observers.forEach { it.onUpdate(mediaList.toList()) }
    }

    fun addObserver(observer: UrlObserver) {
        observers.add(observer)
        if (mediaList.isNotEmpty()) {
            observer.onUpdate(mediaList.toList())
        }
    }

    fun removeObserver(observer: UrlObserver) {
        observers.remove(observer)
    }

    fun clear() {
        mediaList.clear()
    }

    private fun MediaMetadataCompat.Builder.from(
        url: String,
        musicMeta: MusicMeta,
    ): MediaMetadataCompat.Builder {
        id = url
        var titleString = musicMeta.title
        if (titleString == null) {
            titleString =
                try {
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
