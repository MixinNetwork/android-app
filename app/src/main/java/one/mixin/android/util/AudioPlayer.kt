package one.mixin.android.util

import android.media.AudioManager
import android.media.browse.MediaBrowser
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.BuildConfig
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.db.MixinDatabase
import one.mixin.android.event.ProgressEvent.Companion.errorEvent
import one.mixin.android.event.ProgressEvent.Companion.pauseEvent
import one.mixin.android.event.ProgressEvent.Companion.playEvent
import one.mixin.android.event.RecallEvent
import one.mixin.android.extension.fileExists
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.openMedia
import one.mixin.android.extension.toast
import one.mixin.android.session.Session
import one.mixin.android.ui.player.FloatingPlayer
import one.mixin.android.ui.player.internal.album
import one.mixin.android.ui.player.internal.downloadStatus
import one.mixin.android.ui.player.internal.flag
import one.mixin.android.ui.player.internal.id
import one.mixin.android.ui.player.internal.mediaUri
import one.mixin.android.ui.player.internal.title
import one.mixin.android.ui.player.internal.toMediaSource
import one.mixin.android.ui.player.isMusicServiceRunning
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.isData
import one.mixin.android.vo.mediaDownloaded
import one.mixin.android.widget.CircleProgress.Companion.STATUS_DONE
import one.mixin.android.widget.CircleProgress.Companion.STATUS_ERROR
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PAUSE
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.concurrent.TimeUnit

class AudioPlayer private constructor() {
    companion object {
        @Synchronized
        fun get(): AudioPlayer {
            if (instance == null) {
                instance = AudioPlayer()
            }
            return instance as AudioPlayer
        }

        private var instance: AudioPlayer? = null

        fun release(checkMusicService: Boolean = true) {
            Timber.d("@@@ release checkMusicService: $checkMusicService")
            if (checkMusicService) {
                if (isMusicServiceRunning(MixinApplication.appContext)) {
                    return
                }
            }
            instance?.let {
                it.recallDisposable?.let { disposable ->
                    if (!disposable.isDisposed) {
                        disposable.dispose()
                    }
                }
                it.exoPlayer.release()
                it.stopTimber()
            }
            instance = null
        }

        fun pause(checkMusicService: Boolean = true) {
            if (checkMusicService) {
                if (isMusicServiceRunning(MixinApplication.appContext)) {
                    return
                }
            }
            instance?.pause()
        }

        fun resume() {
            instance?.resume()
        }

        fun isEnd(): Boolean {
            return instance == null || instance?.status != STATUS_PLAY
        }

        fun switchAudioStreamType(useFrontSpeaker: Boolean) {
            instance?.switchAudioStreamType(useFrontSpeaker)
        }

        fun sameChatAudioFilePlaying(conversationId: String): Boolean {
            val playItem = instance?.messageItem
            return playItem?.conversationId == conversationId && playItem.isData()
        }

        fun isPlay(id: String): Boolean = instance.notNullWithElse({ return it.status == STATUS_PLAY && it.id == id }, false)

        fun isLoaded(id: String): Boolean = instance.notNullWithElse({ it.isLoaded(id) }, false)

        fun seekTo(progress: Int, max: Float = 100f) = instance?.seekTo(progress, max)

        fun getProgress(): Float = instance?.progress ?: 0f

        private var statusListener: StatusListener? = null

        fun setStatusListener(statusListener: StatusListener) {
            this.statusListener = statusListener
        }

        fun preparePlaylist(
            metadataList: List<MediaMetadataCompat>,
            itemToPlay: MediaMetadataCompat?,
            playWhenReady: Boolean,
            playbackStartPositionMs: Long
        ) {
            get().preparePlaylist(metadataList, itemToPlay, playWhenReady, playbackStartPositionMs)
        }

        fun playMusic(messageItem: MessageItem) {
            get().playMusic(messageItem)
        }

        fun play(
            messageItem: MessageItem,
            autoPlayNext: Boolean = true,
            continuePlayOnlyToday: Boolean = false,
            whenPlayNewAudioMessage: ((Message) -> Unit)? = null
        ) {
            get().play(messageItem, autoPlayNext, continuePlayOnlyToday, whenPlayNewAudioMessage)
        }
    }

    private var recallDisposable: Disposable? = null

    init {
        recallDisposable = RxBus.listen(RecallEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("recall: ${it.messageId}")
                if (id == it.messageId) {
                    pause()
                }
            }
    }

    fun switchAudioStreamType(useFrontSpeaker: Boolean) {
        val streamType = if (useFrontSpeaker) {
            AudioManager.STREAM_MUSIC
        } else {
            AudioManager.STREAM_VOICE_CALL
        }
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(streamType)
            .build()
        exoPlayer.setAudioAttributes(audioAttributes, false)
        exoPlayer.volume = 1f
    }

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    private val playerListener = PlayerListener()

    val exoPlayer: SimpleExoPlayer by lazy {
        SimpleExoPlayer.Builder(MixinApplication.appContext).build().apply {
            setAudioAttributes(this@AudioPlayer.audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
        }
    }

    private inner class PlayerListener : Player.EventListener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                id?.let { id -> RxBus.publish(pauseEvent(id)) }
                stopTimber()
                status = STATUS_DONE

                if (autoPlayNext) {
                    checkNext()
                }
            } else if (playbackState == Player.STATE_READY) {
                if (exoPlayer.playWhenReady) {
                    resume()
                } else {
                    pause()
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (playWhenReady) {
                resume()
            } else {
                pause()
            }
        }

        override fun onPlayerError(error: ExoPlaybackException) {
            if (error.cause is UnrecognizedInputFormatException) {
                status = STATUS_ERROR
                id?.let { id -> RxBus.publish(errorEvent(id)) }
                MixinApplication.appContext.toast(R.string.error_not_supported_audio_format)
                messageItem?.let {
                    MixinApplication.appContext.openMedia(it)
                }
            } else {
                status = STATUS_PAUSE
                id?.let { id -> RxBus.publish(pauseEvent(id)) }
            }
            stopTimber()
            exoPlayer.stop()

            reportExoPlayerException("AudioPlayer", error)
        }

        override fun onPositionDiscontinuity(reason: Int) {
            val newMediaItem = exoPlayer.currentMediaItem ?: return
            if (newMediaItem.mediaId != id) {
                id = newMediaItem.mediaId
                resume()
            }
        }
    }

    private val dataSourceFactory: DefaultDataSourceFactory by lazy {
        DefaultDataSourceFactory(
            MixinApplication.appContext,
            Util.getUserAgent(MixinApplication.appContext, BuildConfig.APPLICATION_ID),
            null
        )
    }

    private var id: String? = null
    private var messageItem: MessageItem? = null
    private var status = STATUS_PAUSE
        set(value) {
            if (field != value) {
                field = value
                statusListener?.onStatusChange(value)
            }
        }

    private var autoPlayNext: Boolean = true
    private var continuePlayOnlyToday: Boolean = false

    var currentPlaylistItems: List<MediaMetadataCompat> = emptyList()

    interface StatusListener {
        fun onStatusChange(status: Int)
    }

    fun preparePlaylist(
        metadataList: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playWhenReady: Boolean = true,
        playbackStartPositionMs: Long = 0,
    ) {
        val downloadedList = metadataList.filter { it.downloadStatus == MediaDescriptionCompat.STATUS_DOWNLOADED }
        currentPlaylistItems = downloadedList
        playMusicList(downloadedList, itemToPlay, playWhenReady, playbackStartPositionMs)
    }

    private fun playMusic(messageItem: MessageItem) {
        if (messageItem.mediaUrl == null) {
            MixinApplication.appContext.toast(R.string.error_bad_data)
            return
        } else if (!messageItem.mediaUrl.fileExists()) {
            MixinApplication.appContext.toast(R.string.error_file_exists)
            return
        }

        checkAddToPlaylist(messageItem)

        if (id != messageItem.messageId) {
            id = messageItem.messageId
            this.messageItem = messageItem
            playMusicList(currentPlaylistItems)
        } else if (status == STATUS_DONE || status == STATUS_ERROR) {
            playMusicList(currentPlaylistItems)
        }

        resume()
    }

    fun setMediaSource(metadataList: List<MediaMetadataCompat>) {
        val downloadedList = metadataList.filter { it.downloadStatus == MediaDescriptionCompat.STATUS_DOWNLOADED }
        currentPlaylistItems = downloadedList
        exoPlayer.apply {
            val itemToPlay = currentMediaItem?.mediaId
            val initialWindowIndex = if (itemToPlay == null) 0 else downloadedList.indexOfFirst { it.description.mediaId == itemToPlay }
            val mediaSource = downloadedList.toMediaSource(dataSourceFactory)
            val pos = getCurrentPos()
            setMediaSource(mediaSource)
            prepare()
            seekTo(initialWindowIndex, pos)
        }
    }

    private fun play(
        messageItem: MessageItem,
        autoPlayNext: Boolean = true,
        continuePlayOnlyToday: Boolean = false,
        whenPlayNewAudioMessage: ((Message) -> Unit)? = null
    ) {
        this.autoPlayNext = autoPlayNext
        this.continuePlayOnlyToday = continuePlayOnlyToday
        if (messageItem.mediaUrl == null) {
            MixinApplication.appContext.toast(R.string.error_bad_data)
            return
        } else if (!messageItem.mediaUrl.fileExists()) {
            MixinApplication.appContext.toast(R.string.error_file_exists)
            return
        }
        if (id != messageItem.messageId) {
            id = messageItem.messageId
            this.messageItem = messageItem
            val mediaSource = buildMediaSourceFromMessageItem(messageItem)
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()

            if (autoPlayNext && messageItem.isAudio()) {
                markAudioReadAndCheckNextAudioAvailable(messageItem, whenPlayNewAudioMessage)
            }
        } else if (status == STATUS_DONE || status == STATUS_ERROR) {
            val mediaSource = buildMediaSourceFromMessageItem(messageItem)
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
        }
        resume()
    }

    private fun resume() {
        status = STATUS_PLAY
        if (!exoPlayer.playWhenReady) {
            exoPlayer.playWhenReady = true
        }
        id?.let {
            RxBus.publish(playEvent(it))
        }
        startTimer()

        if (isMusicServiceRunning(MixinApplication.appContext)) {
            FloatingPlayer.getInstance().startAnim()
        }
    }

    private fun pause() {
        status = STATUS_PAUSE
        if (exoPlayer.playWhenReady) {
            exoPlayer.playWhenReady = false
        }
        id?.let { id ->
            RxBus.publish(pauseEvent(id, -1f))
        }
        stopTimber()

        if (isMusicServiceRunning(MixinApplication.appContext)) {
            FloatingPlayer.getInstance().stopAnim()
        }
    }

    private fun isLoaded(id: String): Boolean {
        return this.id == id && status != STATUS_ERROR && status != STATUS_DONE
    }

    private fun seekTo(progress: Int, max: Float = 100f) {
        val p = progress * duration() / max
        exoPlayer.seekTo(p.toLong())
        id?.let { id -> RxBus.publish(playEvent(id, p)) }
    }

    private fun duration() = if (exoPlayer.duration == C.TIME_UNSET) 0 else exoPlayer.duration.toInt()

    private val period = Timeline.Period()

    private fun getCurrentPos(): Long {
        var position = exoPlayer.currentPosition
        val currentTimeline = exoPlayer.currentTimeline
        if (!currentTimeline.isEmpty) {
            position -= currentTimeline.getPeriod(exoPlayer.currentPeriodIndex, period)
                .positionInWindowMs
        }
        return position
    }

    private fun playMusicList(
        metadataList: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat? = null,
        playWhenReady: Boolean = false,
        playbackStartPositionMs: Long = 0,
    ) {
        val initialWindowIndex = if (itemToPlay == null) 0 else metadataList.indexOf(itemToPlay)
        exoPlayer.apply {
            if (itemToPlay != null) {
                id = itemToPlay.description.mediaId
            }
            val currentPlayId = exoPlayer.currentMediaItem?.mediaId
            val changed = itemToPlay?.description?.mediaId != currentPlayId
            this.playWhenReady = playWhenReady
            val index = metadataList.indexOfFirst { it.description.mediaId == currentPlayId }
            if (changed || index == -1 || metadataList.size == 1) {
                val mediaSource = metadataList.toMediaSource(dataSourceFactory)
                setMediaSource(mediaSource)
                prepare()
                seekTo(initialWindowIndex, playbackStartPositionMs)
            } else {
                if (mediaItemCount == 1 && metadataList.size > 1) {
                    val pre = metadataList.subList(0, index).map {
                        it.toMediaSource(dataSourceFactory)
                    }
                    val post = metadataList.subList(index + 1, metadataList.size).map {
                        it.toMediaSource(dataSourceFactory)
                    }

                    addMediaSources(0, pre)
                    addMediaSources(post)
                }
            }

            if (playWhenReady) {
                resume()
            }
        }
    }

    var timerDisposable: Disposable? = null
    var progress = 0f
    private fun startTimer() {
        if (timerDisposable == null) {
            timerDisposable = Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread()).subscribe {
                    if (duration() == 0) {
                        return@subscribe
                    }
                    progress = getCurrentPos().toFloat() / duration()
                    id?.let { id ->
                        RxBus.publish(playEvent(id, progress))
                    }
                }
        }
    }

    private fun stopTimber() {
        timerDisposable?.let {
            if (!it.isDisposed) {
                it.dispose()
            }
        }
        timerDisposable = null
    }

    private fun checkNext() {
        messageItem?.let { item ->
            if (!item.isAudio()) return
            GlobalScope.launch(Dispatchers.IO) {
                val nextMessage = MixinDatabase.getDatabase(MixinApplication.appContext)
                    .messageDao()
                    .findNextAudioMessageItem(item.conversationId, item.createdAt, item.messageId)
                    ?: return@launch
                if (!nextMessage.mediaDownloaded() || !nextMessage.isAudio() || nextMessage.mediaUrl == null) return@launch

                if (continuePlayOnlyToday) {
                    val currentMessageDate = ZonedDateTime.parse(item.createdAt)
                    val nextMessageDate = ZonedDateTime.parse(nextMessage.createdAt)
                    if (currentMessageDate.year != nextMessageDate.year) return@launch
                    if (currentMessageDate.dayOfYear != nextMessageDate.dayOfYear) return@launch
                }

                withContext(Dispatchers.Main) {
                    play(nextMessage)
                }
            }
        }
    }

    private fun markAudioReadAndCheckNextAudioAvailable(
        currentMessage: MessageItem,
        whenPlayNewAction: ((Message) -> Unit)? = null
    ) = GlobalScope.launch(Dispatchers.IO) {
        val messageDao = MixinDatabase.getDatabase(MixinApplication.appContext).messageDao()
        if (currentMessage.mediaStatus == MediaStatus.DONE.name) {
            messageDao.updateMediaStatus(MediaStatus.READ.name, currentMessage.messageId)
        }
        val message = messageDao.findNextAudioMessage(
            currentMessage.conversationId,
            currentMessage.createdAt,
            currentMessage.messageId
        )
            ?: return@launch
        if (message.userId == Session.getAccountId()) return@launch
        if (!mediaDownloaded(message.mediaStatus)) {
            whenPlayNewAction?.invoke(message)
        }
    }

    private fun checkAddToPlaylist(messageItem: MessageItem) {
        if (messageItem.isData() && MimeTypes.isAudio(messageItem.mediaMimeType)) {
            val mediaMetadata = buildFromMessageItem(messageItem)
            if (currentPlaylistItems.isNullOrEmpty() || currentPlaylistItems[0].album != messageItem.conversationId) {
                currentPlaylistItems = listOf(mediaMetadata)
            } else {
                val exists = currentPlaylistItems.find { it.description.mediaId == messageItem.messageId }
                if (exists == null) {
                    currentPlaylistItems = currentPlaylistItems + mediaMetadata
                }
            }
        }
    }

    private fun buildFromMessageItem(messageItem: MessageItem) =
        MediaMetadataCompat.Builder().apply {
            id = messageItem.messageId
            title = messageItem.mediaName
            album = messageItem.conversationId
            mediaUri = messageItem.mediaUrl
            flag = MediaBrowser.MediaItem.FLAG_PLAYABLE
        }.build()

    private fun buildMediaSourceFromMessageItem(messageItem: MessageItem) =
        ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(
            MediaItem.Builder()
                .setMediaId(messageItem.messageId)
                .setUri(messageItem.mediaUrl)
                .build()
        )
}
