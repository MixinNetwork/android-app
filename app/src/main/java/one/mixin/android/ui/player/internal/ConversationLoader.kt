package one.mixin.android.ui.player.internal

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.browse.MediaBrowser
import android.support.v4.media.MediaMetadataCompat
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.toBytes
import one.mixin.android.vo.MessageItem
import timber.log.Timber
import java.io.File

class ConversationLoader(
    private val database: MixinDatabase,
    val conversationId: String,
) : MusicLoader {

    override suspend fun load(): List<MediaMetadataCompat> {
        val start = System.currentTimeMillis()
        val messageItems = database.messageDao().findAudiosByConversationId(conversationId)
        val query = System.currentTimeMillis()
        Timber.d("@@@ ConversationSource conversationId: $conversationId, query cost: ${query - start}")
        val retriever = MediaMetadataRetriever()
        val mediaMetadataCompats = mutableListOf<MediaMetadataCompat>()
        messageItems.forEach { m ->
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

        retriever.release()
        mediaMetadataCompats.forEach { it.description.extras?.putAll(it.bundle) }

        Timber.d("@@@ ConversationSource retrieve cost: ${System.currentTimeMillis() - query},  mediaMetadataCompats size: ${mediaMetadataCompats.size}")
        return mediaMetadataCompats
    }

    fun MediaMetadataCompat.Builder.from(messageItem: MessageItem, url: String, retriever: MediaMetadataRetriever): MediaMetadataCompat.Builder {
        id = messageItem.messageId
        retriever.setDataSource(url)
        val unknownString = MixinApplication.appContext.getString(R.string.unknown)
        title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: messageItem.mediaName ?: unknownString
        artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: unknownString
        album = conversationId
        mediaUri = messageItem.mediaUrl
        flag = MediaBrowser.MediaItem.FLAG_PLAYABLE

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
                "android.resource://one.mixin.messenger/drawable/ic_avatar_place_holder"
            }
        }
        albumArtUri = albumArt
        displayIconUri = albumArt
        return this
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
