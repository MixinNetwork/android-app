package one.mixin.android.ui.player.internal

import android.annotation.SuppressLint
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import androidx.paging.toLiveData
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.fileSize
import one.mixin.android.ui.player.internal.AlbumArtCache.DEFAULT_ALBUM_ART
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.absolutePath

@SuppressLint("UnsafeOptInUsageError")
class ConversationLoader : MusicMetaLoader() {
    fun conversationLiveData(
        conversationId: String,
        db: MixinDatabase,
        pageSize: Int = 10,
        initialLoadKey: Int = 0,
    ): LiveData<PagedList<MediaMetadataCompat>> =
        db.messageDao().findAudiosByConversationId(conversationId)
            .mapByPage { loadMessageItems(it) }
            .toLiveData(
                PagedList.Config.Builder()
                    .setPageSize(pageSize)
                    .setEnablePlaceholders(true)
                    .build(),
                initialLoadKey,
            )

    private fun loadMessageItems(messageItems: List<MessageItem>): List<MediaMetadataCompat> {
        val mediaMetadataCompatList = mutableListOf<MediaMetadataCompat>()
        messageItems.forEach { m -> loadMessageItem(m).let { mediaMetadataCompatList.add(it) } }
        mediaMetadataCompatList.forEach { it.description.extras?.putAll(it.bundle) }
        return mediaMetadataCompatList
    }

    private fun loadMessageItem(m: MessageItem): MediaMetadataCompat {
        if (ignoreSet.contains(m.messageId)) return MediaMetadataCompat.Builder().from(m).build()

        return when (m.mediaStatus) {
            MediaStatus.CANCELED.name, MediaStatus.PENDING.name -> {
                MediaMetadataCompat.Builder()
                    .from(m)
                    .build()
            }
            else -> {
                val url = m.absolutePath() ?: return MediaMetadataCompat.Builder().from(m).build()
                val musicMeta = retrieveMetadata(m.messageId, url) ?: return MediaMetadataCompat.Builder().from(m).build()
                MediaMetadataCompat.Builder()
                    .from(m, musicMeta)
                    .build()
            }
        }
    }

    fun MediaMetadataCompat.Builder.from(messageItem: MessageItem): MediaMetadataCompat.Builder {
        id = messageItem.messageId
        title = messageItem.mediaName
        val subtitle = messageItem.mediaSize?.fileSize()
        artist = subtitle
        album = messageItem.conversationId
        mediaUri = messageItem.absolutePath()
        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        messageItem.mediaStatus?.let { s ->
            downloadStatus = getDownloadStatus(s)
        }
        albumArtUri = DEFAULT_ALBUM_ART
        displayTitle = messageItem.mediaName
        displaySubtitle = subtitle
        displayIconUri = DEFAULT_ALBUM_ART
        return this
    }

    fun MediaMetadataCompat.Builder.from(
        messageItem: MessageItem,
        musicMeta: MusicMeta,
    ): MediaMetadataCompat.Builder {
        id = messageItem.messageId
        val titleString = musicMeta.title ?: messageItem.mediaName ?: unknownString
        val subtitle = musicMeta.artist ?: unknownString
        title = titleString
        artist = subtitle
        album = messageItem.conversationId
        mediaUri = messageItem.absolutePath()
        flag = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
        messageItem.mediaStatus?.let { s ->
            downloadStatus = getDownloadStatus(s)
        }

        albumArtUri = musicMeta.albumArt
        displayTitle = titleString
        displaySubtitle = subtitle
        displayIconUri = musicMeta.albumArt
        return this
    }

    private fun getDownloadStatus(mediaStatus: String?) =
        when (mediaStatus) {
            MediaStatus.CANCELED.name -> MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
            MediaStatus.PENDING.name -> MediaDescriptionCompat.STATUS_DOWNLOADING
            else -> {
                MediaDescriptionCompat.STATUS_DOWNLOADED
            }
        }
}
