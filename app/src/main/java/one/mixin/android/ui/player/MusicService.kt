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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import one.mixin.android.db.MixinDatabase
import one.mixin.android.extension.isServiceRunning
import one.mixin.android.ui.player.internal.BrowseTree
import one.mixin.android.ui.player.internal.ConversationSource
import one.mixin.android.ui.player.internal.MUSIC_BROWSABLE_ROOT
import one.mixin.android.ui.player.internal.MUSIC_PLAYLIST
import one.mixin.android.ui.player.internal.MusicSource
import one.mixin.android.ui.player.internal.PlaylistSource
import one.mixin.android.ui.player.internal.album
import one.mixin.android.ui.player.internal.flag
import one.mixin.android.ui.player.internal.id
import one.mixin.android.util.AudioPlayer
import one.mixin.android.webrtc.EXTRA_CONVERSATION_ID
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    private lateinit var notificationManager: MusicNotificationManager
    private lateinit var musicSource: MusicSource
    private val musicSourceMap = hashMapOf<String, MusicSource>()

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private var currentPlaylist: Array<String>? = null

    @Inject
    lateinit var database: MixinDatabase

    private var isForegroundService = false

    private val musicAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = PlayerEventListener()

    private val browseTree: BrowseTree by lazy {
        BrowseTree(applicationContext, musicSourceMap)
    }

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
        checkSource(parentId, false)
        Timber.d("@@@ musicSource: $musicSource")
        val resultsSent = musicSource.whenReady { successfullyInitialized ->
            Timber.d("@@@ whenReady parentId: $parentId, successfullyInitialized: $successfullyInitialized")
            if (successfullyInitialized) {
                val children = browseTree[parentId]?.map { item ->
                    MediaBrowserCompat.MediaItem(item.description, item.flag)
                }
                result.sendResult(children)
            } else {
                mediaSession.sendSessionEvent(NETWORK_FAILURE, null)
                result.sendResult(null)
            }
        }

        if (!resultsSent) {
            result.detach()
        }
    }

    private fun checkSource(parentId: String, force: Boolean) {
        if (parentId == MUSIC_PLAYLIST) {
            val playlist = currentPlaylist
            if (!playlist.isNullOrEmpty()) {
                musicSource = PlaylistSource(playlist)
                serviceScope.launch(Dispatchers.IO) {
                    musicSource.load()

                    browseTree.addPlaylist(musicSource)
                }
            } else {
                musicSource = PlaylistSource(emptyArray())
            }
        } else {
            if (force ||
                !::musicSource.isInitialized ||
                (::musicSource.isInitialized && (musicSource as? ConversationSource)?.conversationId != parentId)
            ) {
                var exists = musicSourceMap[parentId]
                if (exists == null) {
                    exists = ConversationSource(database, parentId)
                    musicSourceMap[parentId] = exists
                    musicSource = exists
                    serviceScope.launch(Dispatchers.IO) {
                        musicSource.load()

                        browseTree.addMusicSource(exists)
                    }
                } else {
                    musicSource = exists
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
        }

        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            Timber.d("@@@ onPrepareFromMediaId mediaId: $mediaId, playWhenReady: $playWhenReady")
            if (!::musicSource.isInitialized) return

            if (musicSource is PlaylistSource && musicSource.count() == 0) {
                val playlist = extras?.getStringArray(MUSIC_EXTRA_PLAYLIST)
                if (!playlist.isNullOrEmpty()) {
                    currentPlaylist = playlist
                    notifyChildrenChanged(MUSIC_PLAYLIST)
                }
            }
            musicSource.whenReady {
                Timber.d("@@@ onPrepareFromMediaId whenReady")
                prepare(mediaId, playWhenReady, extras, false)
            }
        }

        private fun prepare(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?,
            endPoint: Boolean,
        ) {
            val itemToPlay: MediaMetadataCompat? = musicSource.find { item ->
                item.id == mediaId
            }
            Timber.d("@@@ itemToPlay: $itemToPlay")
            if (itemToPlay == null) {
                if (endPoint) {
                    Timber.w("$TAG Content not found: MediaID=$mediaId")
                    return
                }
                val cid = extras?.getString(EXTRA_CONVERSATION_ID)
                if (cid != null && cid != (musicSource as? ConversationSource)?.conversationId) {
                    checkSource(cid, true)
                    musicSource.whenReady {
                        notifyChildrenChanged(cid)
                        prepare(mediaId, playWhenReady, extras, true)
                    }
                } else {
                    val playlist = extras?.getStringArray(MUSIC_EXTRA_PLAYLIST)
                    Timber.d("@@ prepare playlist: ${playlist?.size}")
                    if (!playlist.isNullOrEmpty()) {
                        currentPlaylist = playlist
                        notifyChildrenChanged(MUSIC_PLAYLIST)
                    }
                }
            } else {
                val playbackStartPositionMs =
                    extras?.getLong(MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS, C.TIME_UNSET)
                        ?: C.TIME_UNSET

                AudioPlayer.preparePlaylist(
                    buildPlaylist(itemToPlay),
                    itemToPlay,
                    playWhenReady,
                    playbackStartPositionMs
                )
            }
        }

        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {
            musicSource.whenReady {
                val metadataList = musicSource.search(query, extras ?: Bundle.EMPTY)
                if (metadataList.isNotEmpty()) {
                    AudioPlayer.preparePlaylist(
                        metadataList,
                        metadataList[0],
                        playWhenReady,
                        playbackStartPositionMs = C.TIME_UNSET
                    )
                }
            }
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
                    currentPlaylist = playlist
                    notifyChildrenChanged(MUSIC_PLAYLIST)
                }
                MUSIC_CMD_STOP -> {
                    notificationManager.hideNotification()
                    stopForeground(true)
                    isForegroundService = false
                    stopSelf()
                }
            }

            return false
        }

        private fun buildPlaylist(item: MediaMetadataCompat): List<MediaMetadataCompat> =
            musicSource.filter { it.album == item.album }
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

const val MUSIC_EXTRA_PLAYLIST = "music_extra_playlist"

val MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS = "playback_start_position_ms"

private const val TAG = "MusicService"
