package one.mixin.android.ui.player.internal

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.toBytes
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import timber.log.Timber
import java.io.File

class ConversationLoader(
    private val database: MixinDatabase,
    val conversationId: String,
) : MusicLoader {

    override suspend fun load(): List<MediaMetadataCompat> {
        val messageItems = database.messageDao().findAudiosByConversationId(conversationId)
        return loadMessageItems(messageItems)
    }

    suspend fun loadByIds(items: Array<String>): List<MediaMetadataCompat> {
        val messageItems = database.messageDao().suspendFindMessagesByIds(conversationId, items.toList())
        return loadMessageItems(messageItems)
    }

    private fun loadMessageItems(messageItems: List<MessageItem>): List<MediaMetadataCompat> {
        val retriever = MediaMetadataRetriever()
        val mediaMetadataCompats = mutableListOf<MediaMetadataCompat>()
        messageItems.forEach { m ->
            when (m.mediaStatus) {
                MediaStatus.CANCELED.name, MediaStatus.PENDING.name -> {
                    mediaMetadataCompats.add(
                        MediaMetadataCompat.Builder()
                            .from(m)
                            .build()
                    )
                }
                else -> {
                    val path = m.mediaUrl?.getFilePath()
                    if (path != null) {
                        try {
                            mediaMetadataCompats.add(
                                MediaMetadataCompat.Builder()
                                    .from(m, path, retriever)
                                    .build()
                            )
                        } catch (e: Exception) {
                            Timber.w(e)
                        }
                    }
                }
            }
        }

        retriever.release()
        mediaMetadataCompats.forEach { it.description.extras?.putAll(it.bundle) }
        return mediaMetadataCompats
    }

    fun MediaMetadataCompat.Builder.from(messageItem: MessageItem): MediaMetadataCompat.Builder {
        id = messageItem.messageId
        title = messageItem.mediaName
        artist = messageItem.mediaSize?.toString()
        album = conversationId
        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        messageItem.mediaStatus?.let { s ->
            downloadStatus = getDownloadStatus(s)
        }
        albumArtUri = DEFAULT_ALBUM_ART
        displayIconUri = DEFAULT_ALBUM_ART
        return this
    }

    fun MediaMetadataCompat.Builder.from(messageItem: MessageItem, url: String, retriever: MediaMetadataRetriever): MediaMetadataCompat.Builder {
        id = messageItem.messageId
        retriever.setDataSource(url)
        val unknownString = MixinApplication.appContext.getString(R.string.unknown)
        title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: messageItem.mediaName ?: unknownString
        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: unknownString
        album = conversationId
        mediaUri = messageItem.mediaUrl
        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        messageItem.mediaStatus?.let { s ->
            downloadStatus = getDownloadStatus(s)
        }

        val cachedFile = getCachedFile(messageItem.messageId)
        val albumArt = if (cachedFile != null) {
            Timber.d("Cache hit for $url at ${cachedFile.absolutePath}")
            cachedFile.absolutePath
        } else {
            val picture = retriever.embeddedPicture
            if (picture != null) {
                val file = File(parentDir(), filenameForId(messageItem.messageId))
                if (file.exists()) {
                    file.delete()
                }
                val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                file.writeBytes(bitmap.toBytes())
                file.absolutePath
            } else {
                DEFAULT_ALBUM_ART
            }
        }
        albumArtUri = albumArt
        displayIconUri = albumArt
        return this
    }

    private fun getDownloadStatus(mediaStatus: String) = when (mediaStatus) {
        MediaStatus.CANCELED.name -> MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
        MediaStatus.PENDING.name -> MediaDescriptionCompat.STATUS_DOWNLOADING
        else -> {
            MediaDescriptionCompat.STATUS_DOWNLOADED
        }
    }

    private fun getCachedFile(id: String): File? {
        val file = File(parentDir(), filenameForId(id))
        return if (file.exists()) {
            file
        } else null
    }

    private fun parentDir(): File {
        val file = File(MixinApplication.appContext.cacheDir, "album_art_cache")
        if (file.isFile) {
            file.delete()
        }
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    private fun filenameForId(messageId: String) =
        "album_art_cache_" + messageId.replace("\\W+".toRegex(), "") + ".jpg"
}

private const val DEFAULT_ALBUM_ART = "android.resource://one.mixin.messenger/drawable/ic_music_place_holder"
