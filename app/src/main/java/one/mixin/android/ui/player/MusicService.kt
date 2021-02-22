package one.mixin.android.ui.player

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.isServiceRunning
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.ui.player.internal.ConversationLoader
import one.mixin.android.ui.player.internal.MUSIC_BROWSABLE_ROOT
import one.mixin.android.ui.player.internal.MUSIC_PLAYLIST
import one.mixin.android.ui.player.internal.MusicTree
import one.mixin.android.ui.player.internal.PlaylistLoader
import one.mixin.android.ui.player.internal.album
import one.mixin.android.ui.player.internal.flag
import one.mixin.android.util.AudioPlayer
import one.mixin.android.webrtc.EXTRA_CONVERSATION_ID
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    private lateinit var notificationManager: MusicNotificationManager

    private val musicTree = MusicTree()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    @Inject
    lateinit var database: MixinDatabase

    private var isForegroundService = false

    private val musicAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = PlayerEventListener()

    override fun onCreate() {
        super.onCreate()

        val sessionActivityPendingIntent = Intent(this, MusicActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }.let { sessionIntent ->
            PendingIntent.getActivity(this, 0, sessionIntent, 0)
        }

        mediaSession = MediaSessionCompat(this, "MusicService")
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }

        sessionToken = mediaSession.sessionToken

        AudioPlayer.get().exoPlayer.apply {
            setAudioAttributes(musicAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
        }

        notificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicNotificationListener()
        )

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(MusicPlaybackPreparer())
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator(mediaSession))
        mediaSessionConnector.setPlayer(AudioPlayer.get().exoPlayer)

        notificationManager.showNotificationForPlayer(AudioPlayer.get().exoPlayer)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        AudioPlayer.get().exoPlayer.stop(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.run {
            isActive = false
            release()
        }
        serviceJob.cancel()
        AudioPlayer.release(false)
        FloatingPlayer.getInstance().hide()
    }

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        if (Process.SYSTEM_UID != clientUid &&
            Process.myUid() != clientUid &&
            clientPackageName != "com.google.android.mediasimulator" &&
            clientPackageName != "com.google.android.projection.gearhead"
        ) {
            return null
        }
        return BrowserRoot(MUSIC_BROWSABLE_ROOT, null)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        Timber.d("@@@ onLoadChildren parentId: $parentId")

        val exists = musicTree[parentId]
        if (exists != null) {
            val children = exists.map { item ->
                MediaBrowserCompat.MediaItem(item.description, item.flag)
            }
            result.sendResult(children)
        } else {
            if (parentId == MUSIC_PLAYLIST) {
                result.detach()
            } else {
                loadConversationMusic(parentId)
                result.detach()
            }
        }
        this.parentId = parentId
    }

    private var parentId: String = MUSIC_BROWSABLE_ROOT
    private var musicLoader: ConversationLoader? = null
    private var loadJob: Job? = null

    private fun loadConversationMusic(
        parentId: String,
        mediaId: String? = null,
        onLoaded: ((MediaMetadataCompat) -> Unit)? = null,
    ) {
        Timber.d("@@@ loadConversationMusic parentId: $parentId, mediaId: $mediaId, musicLoader: ${musicLoader?.conversationId}")
        if (musicLoader != null && musicLoader?.conversationId == parentId && mediaId == null) {
            return
        }

        Timber.d("@@@ load conversation music")
        loadJob?.cancel()
        musicLoader = ConversationLoader(database, parentId)
        loadJob = serviceScope.launch(Dispatchers.IO) {
            musicLoader?.load()?.let { list ->
                musicTree.setItems(list)
                notifyChildrenChanged(parentId)

                if (mediaId != null && onLoaded != null) {
                    list.find { it.description.mediaId == mediaId }?.let {
                        withContext(Dispatchers.Main) {
                            onLoaded.invoke(it)
                        }
                    }
                }
            }
        }
    }

    private fun updateMusicItems(items: Array<String>) {
        if (musicLoader == null) {
            musicLoader = ConversationLoader(database, parentId)
        }
        serviceScope.launch(Dispatchers.IO) {
            musicLoader?.loadByIds(items)?.let { list ->
                musicTree.setItems(list)
                notifyChildrenChanged(parentId)
            }
        }
    }

    private fun loadPlaylist(
        playlist: Array<String>,
        mediaId: String? = null,
        onLoaded: ((MediaMetadataCompat) -> Unit)? = null,
    ) {
        val playlistLoader = PlaylistLoader(playlist)
        serviceScope.launch(Dispatchers.IO) {
            playlistLoader.load().let { list ->
                musicTree.updatePlaylist(list)
                notifyChildrenChanged(MUSIC_PLAYLIST)

                if (mediaId != null && onLoaded != null) {
                    list.find { it.description.mediaId == mediaId }?.let {
                        withContext(Dispatchers.Main) {
                            onLoaded.invoke(it)
                        }
                    }
                }
            }
        }
    }

    private inner class MusicQueueNavigator(
        mediaSession: MediaSessionCompat
    ) : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
            AudioPlayer.get().currentPlaylistItems[windowIndex].description
    }

    private inner class MusicPlaybackPreparer : MediaSessionConnector.PlaybackPreparer {

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH or
                PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH

        override fun onPrepare(playWhenReady: Boolean) {
            Timber.d("@@@ onPrepare playWhenReady: $playWhenReady")
        }

        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            Timber.d("@@@ onPrepareFromMediaId mediaId: $mediaId, playWhenReady: $playWhenReady")
            val parentExists = musicTree[parentId]
            if (parentExists == null) {
                Timber.d("@@@ not find parent for $parentId")
                if (parentId == MUSIC_PLAYLIST) {
                    val playlist = extras?.getStringArray(MUSIC_EXTRA_PLAYLIST)
                    if (!playlist.isNullOrEmpty()) {
                        loadPlaylist(playlist, mediaId) {
                            playItem(it, playWhenReady, extras)
                        }
                    } else {
                        Timber.w("$TAG Content not found: MediaID=$mediaId")
                    }
                } else {
                    val cid = extras?.getString(EXTRA_CONVERSATION_ID)
                    if (cid != null) {
                        loadConversationMusic(cid, mediaId) {
                            playItem(it, playWhenReady, extras)
                        }
                    } else {
                        Timber.w("$TAG Content not found: MediaID=$mediaId")
                    }
                }
            } else {
                Timber.d("@@@ find parent for $parentId")
                val itemToPlay = parentExists.find { item ->
                    item.description.mediaId == mediaId
                }
                if (itemToPlay == null) {
                    if (parentId == MUSIC_PLAYLIST) {
                        val playlist = extras?.getStringArray(MUSIC_EXTRA_PLAYLIST)
                        if (!playlist.isNullOrEmpty()) {
                            loadPlaylist(playlist, mediaId) {
                                playItem(it, playWhenReady, extras)
                            }
                        } else {
                            Timber.w("$TAG Content not found: MediaID=$mediaId")
                        }
                    } else {
                        val cid = extras?.getString(EXTRA_CONVERSATION_ID)
                        if (cid != null) {
                            loadConversationMusic(cid, mediaId) {
                                playItem(it, playWhenReady, extras)
                            }
                        } else {
                            Timber.w("$TAG Content not found: MediaID=$mediaId")
                        }
                    }
                } else {
                    playItem(itemToPlay, playWhenReady, extras)
                }
            }
        }

        private fun playItem(
            itemToPlay: MediaMetadataCompat,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            val playbackStartPositionMs =
                extras?.getLong(MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS, C.TIME_UNSET)
                    ?: C.TIME_UNSET

            val playlist = buildPlaylist(itemToPlay)
            Timber.d("@@@ playlist size: ${playlist.size}")
            AudioPlayer.preparePlaylist(
                playlist,
                itemToPlay,
                playWhenReady,
                playbackStartPositionMs
            )
        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {
        }

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit

        override fun onCommand(
            player: Player,
            controlDispatcher: ControlDispatcher,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ): Boolean {
            Timber.d("@@@ onCommand command: $command")
            when (command) {
                MUSIC_CMD_PLAYLIST -> {
                    val playlist = extras?.getStringArray(MUSIC_EXTRA_PLAYLIST) ?: return false
                    loadPlaylist(playlist, playlist[0]) {
                        playItem(it, true, null)
                    }
                }
                MUSIC_CMD_STOP -> {
                    notificationManager.hideNotification()
                    stopForeground(true)
                    isForegroundService = false
                    stopSelf()
                }
                MUSIC_CMD_UPDATE_ITEMS -> {
                    val items = extras?.getStringArray(MUSIC_EXTRA_ITEMS) ?: return false
                    updateMusicItems(items)
                }
            }

            return false
        }

        private fun buildPlaylist(item: MediaMetadataCompat): List<MediaMetadataCompat> =
            item.album.notNullWithElse<String, List<MediaMetadataCompat>>(
                {
                    musicTree[it] ?: emptyList()
                },
                {
                    emptyList()
                }
            )
    }

    private inner class MusicNotificationListener : PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(
                    applicationContext,
                    Intent(applicationContext, this@MusicService.javaClass)
                )

                startForeground(notificationId, notification)
                isForegroundService = true
            }
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }

    private inner class PlayerEventListener : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    notificationManager.showNotificationForPlayer(AudioPlayer.get().exoPlayer)
                    if (playbackState == Player.STATE_READY) {
                        if (!playWhenReady) {
                            stopForeground(false)
                        }
                    }
                }
                else -> {
                    notificationManager.hideNotification()
                }
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            Timber.e(error)
            // reportExoPlayerException("MusicPlayer", error)
        }
    }
}

fun isMusicServiceRunning(context: Context) = context.isServiceRunning(MusicService::class.java)

const val NETWORK_FAILURE = "one.mixin.messenger.media.session.NETWORK_FAILURE"

const val MUSIC_CMD_PLAYLIST = "music_cmd_playlist"
const val MUSIC_CMD_STOP = "music_cmd_stop"
const val MUSIC_CMD_UPDATE_ITEMS = "music_cmd_update_items"

const val MUSIC_EXTRA_PLAYLIST = "music_extra_playlist"
const val MUSIC_EXTRA_ITEMS = "music_extra_items"

val MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS = "playback_start_position_ms"

private const val TAG = "MusicService"
