package one.mixin.android.ui.player.internal

import android.annotation.SuppressLint
import android.util.LruCache
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.exoplayer.MetadataRetriever
import androidx.media3.extractor.metadata.flac.PictureFrame
import androidx.media3.extractor.metadata.id3.ApicFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.extractor.metadata.vorbis.VorbisComment
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.isLocalScheme
import one.mixin.android.extension.toUri
import one.mixin.android.util.getLocalString
import timber.log.Timber
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

abstract class MusicMetaLoader {
    val unknownString = getLocalString(MixinApplication.appContext, R.string.Unknown)

    val ignoreSet = mutableSetOf<String>()

    private val metaCache = LruCache<String, MusicMeta>(100 * 1024)

    @SuppressLint("UnsafeOptInUsageError")
    protected fun retrieveMetadata(
        id: String,
        url: String,
        timeoutMillis: Long = RETRIEVE_TIMEOUT_MILLI_SEC,
    ): MusicMeta? {
        val hit = metaCache[url]
        if (hit != null) return hit

        try {
            val item = MediaItem.fromUri(url)
            val trackGroupsFuture = MetadataRetriever.retrieveMetadata(MixinApplication.appContext, item)
            val trackGroups = trackGroupsFuture.get(timeoutMillis, TimeUnit.MILLISECONDS)
            for (i in 0 until trackGroups.length) {
                val trackGroup = trackGroups[i]
                for (j in 0 until trackGroup.length) {
                    val trackMetadata = trackGroup.getFormat(j).metadata
                    if (trackMetadata != null) {
                        val meta = decodeMetadata(id, url, trackMetadata)
                        metaCache.put(url, meta)
                        return meta
                    }
                }
            }
        } catch (e: Exception) {
            if (e is FileNotFoundException || e is SecurityException) {
                // message exits but media not
                ignoreSet.add(id)
                return null
            } else if (e is TimeoutException) {
                return if (url.toUri().scheme?.isLocalScheme() == false) {
                    EMPTY_MUSIC_META
                } else {
                    ignoreSet.add(id)
                    null
                }
            }
            Timber.w(e)
            return EMPTY_MUSIC_META
        }
        return null
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun decodeMetadata(
        id: String,
        url: String,
        metadata: Metadata,
    ): MusicMeta {
        val artistList = mutableSetOf<String>()
        var title: String? = null
        var album: String? = null
        var albumArt: String? = null
        for (i in 0 until metadata.length()) {
            when (val entry = metadata[i]) {
                is TextInformationFrame -> {
                    when {
                        entry.id.startsWith("TPE") -> {
                            artistList.add(entry.value)
                        }
                        entry.id == "TALB" -> {
                            album = entry.value
                        }
                        entry.id.startsWith("TIT") -> {
                            title = entry.value
                        }
                    }
                }
                is ApicFrame -> {
                    albumArt = AlbumArtCache.getAlbumArtUri(id, entry.pictureData)
                }
                is PictureFrame -> {
                    albumArt = AlbumArtCache.getAlbumArtUri(id, entry.pictureData)
                }
                is VorbisComment -> {
                    when (entry.key) {
                        "ALBUM" -> {
                            album = entry.value
                        }
                        "ARTIST" -> {
                            artistList.add(entry.value)
                        }
                        "TITLE" -> {
                            title = entry.value
                        }
                    }
                }
            }
        }
        val artist =
            if (artistList.isNotEmpty()) {
                artistList.joinToString { it }
            } else {
                null
            }
        return MusicMeta(title, album, albumArt, artist)
    }

    data class MusicMeta(
        val title: String?,
        val album: String?,
        val albumArt: String?,
        val artist: String?,
    )

    @Suppress("PropertyName")
    val EMPTY_MUSIC_META = MusicMeta(null, null, null, null)

    companion object {
        private const val RETRIEVE_TIMEOUT_MILLI_SEC = 100L
    }
}
