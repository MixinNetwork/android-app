package one.mixin.android.ui.player

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagedList
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_INTERNAL
import com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_REMOVE
import com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import one.mixin.android.RxBus
import one.mixin.android.db.MixinDatabase
import one.mixin.android.event.ProgressEvent.Companion.playEvent
import one.mixin.android.extension.isServiceRunning
import one.mixin.android.ui.player.internal.ConversationLoader
import one.mixin.android.ui.player.internal.UrlLoader
import one.mixin.android.ui.player.internal.currentMediaItems
import one.mixin.android.ui.player.internal.downloadStatus
import one.mixin.android.ui.player.internal.id
import one.mixin.android.ui.player.internal.urlLoader
import one.mixin.android.util.MusicPlayer
import one.mixin.android.util.debug.measureTimeMillis
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

@AndroidEntryPoint
class MusicService : LifecycleService() {
    private lateinit var notificationManager: MusicNotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private val playerListener = PlayerEventListener()

    private var albumId: String = ""
    private val conversationLoader = ConversationLoader()
    private var currentPlaylist: List<MediaMetadataCompat>? = null

    @Inject
    lateinit var db: MixinDatabase

    override fun onCreate() {
        super.onCreate()
        val sessionActivityPendingIntent = Intent(this, MusicActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }.let { sessionIntent ->
            PendingIntent.getActivity(this, 0, sessionIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        mediaSession = MediaSessionCompat(this, "MusicService")
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
            }

        MusicPlayer.get().exoPlayer.addListener(playerListener)

        notificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicNotificationListener()
        )

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator(mediaSession))
        mediaSessionConnector.setPlayer(MusicPlayer.get().exoPlayer)

        notificationManager.showNotificationForPlayer(MusicPlayer.get().exoPlayer)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.action
        if (intent == null || action == null) {
            return START_NOT_STICKY
        }

        lifecycleScope.launch {
            when (action) {
                ACTION_PLAY_CONVERSATION -> handleConversation(intent)
                ACTION_PLAY_URLS -> handleUrls(intent)
                ACTION_STOP_MUSIC -> handleStopMusic()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        urlObserver?.let { urlLoader.removeObserver(it) }
        urlLoader.clear()
        if (::conversationObserver.isInitialized) {
            conversationLiveData?.removeObserver(conversationObserver)
        }
        conversationLiveData = null
        currentPlaylist = null

        mediaSession.run {
            isActive = false
            release()
        }

        MusicPlayer.get().exoPlayer.removeListener(playerListener)
        MusicPlayer.release()
        FloatingPlayer.getInstance().hide(true)
    }

    private fun handleStopMusic() {
        notificationManager.hideNotification()
    }

    private var conversationLiveData: LiveData<PagedList<MediaMetadataCompat>>? = null
    private lateinit var conversationObserver: ConversationObserver
    private var urlObserver: UrlObserver? = null

    private suspend fun handleConversation(intent: Intent) {
        val albumId = intent.getStringExtra(EXTRA_ALBUM_ID) ?: return
        val mediaId = intent.getStringExtra(EXTRA_MEDIA_ID)
        handleConversation(albumId, mediaId)
    }

    private suspend fun handleConversation(albumId: String, mediaId: String?) {
        if (albumId == this.albumId) {
            if (mediaId == null) return

            if (mediaId != MusicPlayer.get().currentPlayMediaId()) {
                val exists = MusicPlayer.get().exoPlayer.currentMediaItems.find { it.mediaId == mediaId }
                if (exists == null) {
                    RxBus.publish(playEvent(mediaId)) // respond UI before load

                    val index = db.messageDao().indexAudioByConversationId(mediaId, albumId)
                    Timber.d("@@@ not found $mediaId, new index: $index")
                    conversationObserver.loadAround(index, mediaId)
                } else {
                    MusicPlayer.get().playMediaById(mediaId)
                }
            } else {
                MusicPlayer.resume()
            }
            return
        }

        this.albumId = albumId
        MusicPlayer.resetModeAndSpeed()
        urlObserver?.let { urlLoader.removeObserver(it) }
        urlLoader.clear()

        if (::conversationObserver.isInitialized) {
            conversationLiveData?.removeObserver(conversationObserver)
        }

        mediaId?.let { RxBus.publish(playEvent(it)) } // respond UI before load

        conversationObserver = ConversationObserver(mediaId)
        val initialLoadKey = if (mediaId != null) {
            measureTimeMillis("@@@ index cost: ") {
                db.messageDao().indexAudioByConversationId(mediaId, albumId)
            }
        } else 0
        Timber.d("@@@ initialLoadKey: $initialLoadKey")
        conversationLiveData = conversationLoader.conversationLiveData(albumId, db, initialLoadKey = initialLoadKey)
        conversationLiveData?.observe(this, conversationObserver)
    }

    private inner class ConversationObserver(private var mediaId: String? = null) : Observer<PagedList<MediaMetadataCompat>> {
        private var first = true
        private var currentPagedList: PagedList<MediaMetadataCompat>? = null
        private var playWhenReady = true

        override fun onChanged(pagedList: PagedList<MediaMetadataCompat>) {
            Timber.d("@@@ pagedList size: ${pagedList.size}")
            currentPagedList = pagedList
            val downloadedList = mutableListOf<MediaMetadataCompat>()
            for (i in 0 until pagedList.size) {
                val item = pagedList[i]
                if (item != null && item.downloadStatus == MediaDescriptionCompat.STATUS_DOWNLOADED) {
                    downloadedList.add(item)
                }
            }
            Timber.d("@@@ downloadedList size: ${downloadedList.size}")
            currentPlaylist = downloadedList
            lifecycleScope.launch {
                MusicPlayer.get().updatePlaylist(downloadedList)

                if (first) {
                    first = false
                    mediaId?.let {
                        MusicPlayer.get().playMediaById(it, playWhenReady)
                    }
                }
            }
        }

        fun loadAround(index: Int, mediaId: String, playWhenReady: Boolean = true) {
            currentPagedList?.let { list ->
                list.loadAround(max(0, min(list.size - 1, index)))
                this.mediaId = mediaId
                this.playWhenReady = playWhenReady
                first = true
                list.dataSource.invalidate()
            }
        }
    }

    private suspend fun handleUrls(intent: Intent) {
        val urls = intent.getStringArrayExtra(EXTRA_URLS)
        if (urls.isNullOrEmpty()) {
            return
        }

        if (::conversationObserver.isInitialized) {
            conversationLiveData?.removeObserver(conversationObserver)
        }
        conversationLiveData = null

        this@MusicService.albumId = MUSIC_PLAYLIST
        MusicPlayer.resetModeAndSpeed()
        urlObserver?.let { urlLoader.removeObserver(it) }

        urlObserver = UrlObserver().apply {
            urlLoader.addObserver(this)
        }
        urlLoader.load(urls)
    }

    private inner class UrlObserver : UrlLoader.UrlObserver {
        private var first = true

        override fun onUpdate(mediaList: List<MediaMetadataCompat>) {
            currentPlaylist = mediaList
            lifecycleScope.launch {
                MusicPlayer.get().updatePlaylist(mediaList)

                if (mediaList.isNotEmpty() && first) {
                    first = false
                    mediaList.firstOrNull()?.id?.let {
                        MusicPlayer.get().playMediaById(it)
                    }
                }
            }
        }
    }

    private inner class MusicQueueNavigator(
        mediaSession: MediaSessionCompat
    ) : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return try {
                currentPlaylist?.get(windowIndex)?.description
                    ?: MediaDescriptionCompat.Builder().setMediaId(MUSIC_UNKNOWN_ROOT).build()
            } catch (e: IndexOutOfBoundsException) {
                Timber.w(e)
                MediaDescriptionCompat.Builder().setMediaId(MUSIC_UNKNOWN_ROOT).build()
            }
        }
    }

    private inner class MusicNotificationListener : PlayerNotificationManager.NotificationListener {
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            ContextCompat.startForegroundService(
                applicationContext,
                Intent(applicationContext, this@MusicService.javaClass)
            )

            startForeground(notificationId, notification)
        }

        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            stopSelf()
        }
    }

    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    val player = MusicPlayer.get().exoPlayer
                    notificationManager.showNotificationForPlayer(player)
                }
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            if (reason == DISCONTINUITY_REASON_SEEK_ADJUSTMENT ||
                reason == DISCONTINUITY_REASON_REMOVE ||
                reason == DISCONTINUITY_REASON_INTERNAL
            ) {
                return
            }

            if (albumId != MUSIC_PLAYLIST && !currentPlaylist.isNullOrEmpty()) {
                val oldPos = oldPosition.mediaItemIndex
                val newPos = newPosition.mediaItemIndex
                if (oldPos != newPos && newPos == 0 || newPos == (currentPlaylist?.size ?: 0) - 1) {
                    lifecycleScope.launch {
                        val item = newPosition.mediaItem ?: return@launch
                        val index = db.messageDao().indexAudioByConversationId(item.mediaId, albumId)
                        conversationObserver.loadAround(index, item.mediaId, false)
                    }
                }
            }
        }
    }

    companion object {
        fun isRunning(context: Context) = context.isServiceRunning(MusicService::class.java)

        fun playConversation(context: Context, albumId: String, mediaId: String) = startService(context, ACTION_PLAY_CONVERSATION) {
            putExtra(EXTRA_ALBUM_ID, albumId)
            putExtra(EXTRA_MEDIA_ID, mediaId)
        }

        fun playUrls(context: Context, urls: Array<String>) = startService(context, ACTION_PLAY_URLS) {
            putExtra(EXTRA_URLS, urls)
        }

        fun stopMusic(context: Context) = startService(context, ACTION_STOP_MUSIC)

        fun startService(
            ctx: Context,
            action: String? = null,
            putExtra: (Intent.() -> Unit)? = null
        ) {
            val intent = Intent(ctx, MusicService::class.java).apply {
                this.action = action
                putExtra?.invoke(this)
            }
            ctx.startService(intent)
        }

        const val ACTION_PLAY_CONVERSATION = "action_play_conversation"
        const val EXTRA_ALBUM_ID = "extra_album_id"
        const val EXTRA_MEDIA_ID = "extra_media_id"

        const val ACTION_PLAY_URLS = "action_play_urls"
        const val EXTRA_URLS = "extra_urls"

        const val ACTION_STOP_MUSIC = "action_stop_music"

        const val MUSIC_UNKNOWN_ROOT = "__UNKNOWN__"
        const val MUSIC_PLAYLIST = "__PLAYLIST_"
    }
}
