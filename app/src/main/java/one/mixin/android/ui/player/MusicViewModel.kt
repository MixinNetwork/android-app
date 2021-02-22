package one.mixin.android.ui.player

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import one.mixin.android.R
import one.mixin.android.extension.observeUntil
import one.mixin.android.ui.player.internal.EMPTY_PLAYBACK_STATE
import one.mixin.android.ui.player.internal.MusicServiceConnection
import one.mixin.android.ui.player.internal.NOTHING_PLAYING
import one.mixin.android.ui.player.internal.id
import one.mixin.android.ui.player.internal.isPlayEnabled
import one.mixin.android.ui.player.internal.isPlaying
import one.mixin.android.ui.player.internal.isPrepared
import one.mixin.android.webrtc.EXTRA_CONVERSATION_ID
import timber.log.Timber

class MusicViewModel
internal constructor(
    private val mediaId: String,
    musicServiceConnection: MusicServiceConnection,
) : ViewModel() {

    private val _mediaItems = MutableLiveData<List<MediaItemData>>()
    val mediaItems: LiveData<List<MediaItemData>> = _mediaItems

    /**
     * Pass the status of the [MusicServiceConnection.networkFailure] through.
     */
    val networkError = Transformations.map(musicServiceConnection.networkFailure) { it }

    fun playMedia(mediaItem: MediaItemData, pauseAllowed: Boolean = true, onChildrenLoaded: () -> Unit) {
        checkConnected {
            val nowPlaying = musicServiceConnection.nowPlaying.value
            val transportControls = musicServiceConnection.transportControls

            val isPrepared = musicServiceConnection.playbackState.value?.isPrepared ?: false
            if (isPrepared && mediaItem.mediaId == nowPlaying?.id) {
                musicServiceConnection.playbackState.value?.let { playbackState ->
                    when {
                        playbackState.isPlaying ->
                            if (pauseAllowed) transportControls.pause() else Unit
                        playbackState.isPlayEnabled -> transportControls.play()
                        else -> {
                            Timber.w("$TAG Playable item clicked but neither play nor pause are enabled! (mediaId=${mediaItem.mediaId})")
                        }
                    }
                }
            } else {
                transportControls.playFromMediaId(
                    mediaItem.mediaId,
                    Bundle().apply {
                        putString(EXTRA_CONVERSATION_ID, mediaId)
                    }
                )

                musicServiceConnection.subscribe(
                    mediaId,
                    object : MediaBrowserCompat.SubscriptionCallback() {
                        override fun onChildrenLoaded(parentId: String, children: MutableList<MediaBrowserCompat.MediaItem>) {
                            Timber.d("@@@ onChildrenLoaded")
                            onChildrenLoaded.invoke()
                            musicServiceConnection.unsubscribe(mediaId, this)
                        }
                    }
                )
            }
        }
    }

    fun showPlaylist(playlist: Array<String>, onConnected: () -> Unit) {
        checkConnected {
            val transportControls = musicServiceConnection.transportControls
            transportControls.playFromMediaId(
                playlist[0],
                Bundle().apply {
                    putStringArray(MUSIC_EXTRA_PLAYLIST, playlist)
                }
            )
            // val bundle = Bundle().apply {
            //     putStringArray(MUSIC_EXTRA_PLAYLIST, playlist)
            // }
            // musicServiceConnection.sendCommand(MUSIC_CMD_PLAYLIST, bundle)

            onConnected.invoke()
        }
    }

    fun subscribe() {
        checkConnected {
            musicServiceConnection.subscribe(mediaId, subscriptionCallback)
        }
    }

    fun stopMusicService() {
        musicServiceConnection.sendCommand(MUSIC_CMD_STOP, null)
        musicServiceConnection.disconnect()
    }

    private fun checkConnected(onConnected: () -> Unit) {
        if (musicServiceConnection.isConnected.value == true) {
            onConnected.invoke()
            return
        }
        musicServiceConnection.connect()
        musicServiceConnection.isConnected.observeUntil(true) { isConnected ->
            if (isConnected) {
                onConnected.invoke()
            }
        }
    }

    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        override fun onChildrenLoaded(parentId: String, children: List<MediaBrowserCompat.MediaItem>) {
            Timber.d("@@@ onChildrenLoaded parentId: $parentId")
            val itemsList = children.map { child ->
                val subtitle = child.description.subtitle ?: ""
                MediaItemData(
                    child.mediaId!!,
                    child.description.title.toString(),
                    subtitle.toString(),
                    child.description.iconUri!!,
                    child.isBrowsable,
                    getResourceForMediaId(child.mediaId!!)
                )
            }
            _mediaItems.postValue(itemsList)
        }
    }

    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        val playbackState = it ?: EMPTY_PLAYBACK_STATE
        val metadata = musicServiceConnection.nowPlaying.value ?: NOTHING_PLAYING
        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) != null) {
            _mediaItems.postValue(updateState(playbackState, metadata))
        }
    }

    /**
     * When the session's [MediaMetadataCompat] changes, the [mediaItems] need to be updated
     * as it means the currently active item has changed. As a result, the new, and potentially
     * old item (if there was one), both need to have their [MediaItemData.playbackRes]
     * changed. (i.e.: play/pause button or blank)
     */
    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        val playbackState = musicServiceConnection.playbackState.value ?: EMPTY_PLAYBACK_STATE
        val metadata = it ?: NOTHING_PLAYING
        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) != null) {
            _mediaItems.postValue(updateState(playbackState, metadata))
        }
    }

    /**
     * Because there's a complex dance between this [ViewModel] and the [MusicServiceConnection]
     * (which is wrapping a [MediaBrowserCompat] object), the usual guidance of using
     * [Transformations] doesn't quite work.
     *
     * Specifically there's three things that are watched that will cause the single piece of
     * [LiveData] exposed from this class to be updated.
     *
     * [subscriptionCallback] (defined above) is called if/when the children of this
     * ViewModel's [mediaId] changes.
     *
     * [MusicServiceConnection.playbackState] changes state based on the playback state of
     * the player, which can change the [MediaItemData.playbackRes]s in the list.
     *
     * [MusicServiceConnection.nowPlaying] changes based on the item that's being played,
     * which can also change the [MediaItemData.playbackRes]s in the list.
     */
    private val musicServiceConnection = musicServiceConnection.also {
        it.subscribe(mediaId, subscriptionCallback)

        it.playbackState.observeForever(playbackStateObserver)
        it.nowPlaying.observeForever(mediaMetadataObserver)
    }

    /**
     * Since we use [LiveData.observeForever] above (in [musicServiceConnection]), we want
     * to call [LiveData.removeObserver] here to prevent leaking resources when the [ViewModel]
     * is not longer in use.
     *
     * For more details, see the kdoc on [musicServiceConnection] above.
     */
    override fun onCleared() {
        super.onCleared()

        // Remove the permanent observers from the MusicServiceConnection.
        musicServiceConnection.playbackState.removeObserver(playbackStateObserver)
        musicServiceConnection.nowPlaying.removeObserver(mediaMetadataObserver)

        // And then, finally, unsubscribe the media ID that was being watched.
        musicServiceConnection.unsubscribe(mediaId, subscriptionCallback)
    }

    private fun getResourceForMediaId(mediaId: String): Int {
        val isActive = mediaId == musicServiceConnection.nowPlaying.value?.id
        val isPlaying = musicServiceConnection.playbackState.value?.isPlaying ?: false
        return when {
            !isActive -> NO_RES
            isPlaying -> R.drawable.exo_icon_pause
            else -> R.drawable.exo_icon_play
        }
    }

    private fun updateState(
        playbackState: PlaybackStateCompat,
        mediaMetadata: MediaMetadataCompat
    ): List<MediaItemData> {

        val newResId = when (playbackState.isPlaying) {
            true -> R.drawable.exo_icon_pause
            else -> R.drawable.exo_icon_play
        }

        return mediaItems.value?.map {
            val useResId = if (it.mediaId == mediaMetadata.id) newResId else NO_RES
            it.copy(playbackRes = useResId)
        } ?: emptyList()
    }

    class Factory(
        private val mediaId: String,
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return MusicViewModel(mediaId, musicServiceConnection) as T
        }
    }
}

fun provideMusicViewModel(musicServiceConnection: MusicServiceConnection, mediaId: String):
    MusicViewModel.Factory {
        return MusicViewModel.Factory(mediaId, musicServiceConnection)
    }

private const val NO_RES = 0
private const val TAG = "MusicViewModel"
