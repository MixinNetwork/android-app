package one.mixin.android.util

import android.annotation.SuppressLint
import android.support.v4.media.MediaMetadataCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlaybackException
import androidx.media3.exoplayer.ExoPlaybackException.TYPE_SOURCE
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.event.ProgressEvent.Companion.errorEvent
import one.mixin.android.event.ProgressEvent.Companion.pauseEvent
import one.mixin.android.event.ProgressEvent.Companion.playEvent
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.toast
import one.mixin.android.ui.player.FloatingPlayer
import one.mixin.android.ui.player.MusicService
import one.mixin.android.ui.player.internal.MusicPlayerUpdater
import one.mixin.android.ui.player.internal.currentMediaItems
import one.mixin.android.widget.CircleProgress.Companion.STATUS_DONE
import one.mixin.android.widget.CircleProgress.Companion.STATUS_ERROR
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PAUSE
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY
import java.util.concurrent.TimeUnit

class MusicPlayer private constructor() {
    companion object {
        @Synchronized
        fun get(): MusicPlayer {
            if (instance == null) {
                instance = MusicPlayer()
            }
            return instance as MusicPlayer
        }

        private var instance: MusicPlayer? = null

        fun release() {
            instance?.let {
                it.exoPlayer.release()
                it.stopTimber()
            }
            instance = null
        }

        fun resume() {
            instance?.resume()
        }

        fun pause() {
            instance?.pause()
        }

        fun isPlay(id: String): Boolean = instance.notNullWithElse({ return it.status == STATUS_PLAY && it.id() == id }, false)

        fun seekTo(
            progress: Int,
            max: Float = 100f,
        ) = instance?.seekTo(progress, max)

        fun resetModeAndSpeed() {
            instance?.exoPlayer?.let {
                it.playbackParameters = PlaybackParameters(1f)
                it.repeatMode = Player.REPEAT_MODE_OFF
            }
        }
    }

    private val audioAttributes =
        AudioAttributes.Builder()
            .setContentType(AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

    private val playerListener = PlayerListener()

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(MixinApplication.appContext).build().apply {
            setAudioAttributes(this@MusicPlayer.audioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerListener)
        }
    }

    private val updater = MusicPlayerUpdater(exoPlayer)

    private inner class PlayerListener : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                id()?.let { id -> RxBus.publish(pauseEvent(id)) }
                stopTimber()
                status = STATUS_DONE
                // Resume progress when player finished playing
                seekTo(0)
                pause()
                if (MusicService.isRunning(MixinApplication.appContext)) {
                    FloatingPlayer.getInstance().stopAnim()
                }
            } else if (playbackState == Player.STATE_READY) {
                if (exoPlayer.playWhenReady) {
                    resume()
                } else {
                    pause()
                }
            }
        }

        override fun onPlayWhenReadyChanged(
            playWhenReady: Boolean,
            reason: Int,
        ) {
            if (playWhenReady) {
                AudioPlayer.pause()
                resume()
            } else {
                pause()
            }
        }

        @SuppressLint("UnsafeOptInUsageError")
        override fun onPlayerError(error: PlaybackException) {
            if (error.cause is UnrecognizedInputFormatException) {
                status = STATUS_ERROR
                id()?.let { id -> RxBus.publish(errorEvent(id)) }
                toast(R.string.error_not_supported_audio_format)
            } else {
                if (error is ExoPlaybackException && error.type == TYPE_SOURCE) {
                    toast(R.string.Playback_failed)
                }

                status = STATUS_PAUSE
                id()?.let { id -> RxBus.publish(pauseEvent(id)) }
            }
            stopTimber()
            exoPlayer.pause()

            reportExoPlayerException("MusicPlayer", error)
        }
    }

    private var status = STATUS_PAUSE

    private fun id(): String? = exoPlayer.currentMediaItem?.mediaId

    suspend fun updatePlaylist(mediaMetadataCompatList: List<MediaMetadataCompat>) {
        updater.update(mediaMetadataCompatList)
    }

    fun playMediaById(
        mediaId: String,
        playWhenReady: Boolean = true,
    ) {
        val index = exoPlayer.currentMediaItems.indexOfFirst { it.mediaId == mediaId }
        if (index == -1) return

        val currentIndex = exoPlayer.currentMediaItemIndex
        if (index != currentIndex) {
            exoPlayer.seekToDefaultPosition(index)
        }
        if (!exoPlayer.playWhenReady) {
            resume(playWhenReady)
        }
    }

    fun currentPlayMediaId(): String? = notNullWithElse({ return id() }, null)

    private fun resume(playWhenReady: Boolean = true) {
        status = STATUS_PLAY
        if (playWhenReady && !exoPlayer.playWhenReady) {
            exoPlayer.playWhenReady = true
        }
        id()?.let {
            RxBus.publish(playEvent(it))
        }
        startTimer()

        if (MusicService.isRunning(MixinApplication.appContext)) {
            FloatingPlayer.getInstance().startAnim()
        }
    }

    private fun pause() {
        status = STATUS_PAUSE
        if (exoPlayer.playWhenReady) {
            exoPlayer.playWhenReady = false
        }
        id()?.let { id ->
            RxBus.publish(pauseEvent(id, -1f))
        }
        stopTimber()

        if (MusicService.isRunning(MixinApplication.appContext)) {
            FloatingPlayer.getInstance().stopAnim()
        }
    }

    private fun seekTo(
        progress: Int,
        max: Float = 100f,
    ) {
        val p = progress * duration() / max
        exoPlayer.seekTo(p.toLong())
        id()?.let { id -> RxBus.publish(playEvent(id, p)) }
    }

    private fun duration() = if (exoPlayer.duration == C.TIME_UNSET) 0 else exoPlayer.duration.toInt()

    private val period = Timeline.Period()

    private fun getCurrentPos(): Long {
        var position = exoPlayer.currentPosition
        val currentTimeline = exoPlayer.currentTimeline
        if (!currentTimeline.isEmpty) {
            position -=
                currentTimeline.getPeriod(exoPlayer.currentPeriodIndex, period)
                    .positionInWindowMs
        }
        return position
    }

    private var timerDisposable: Disposable? = null
    var progress = 0f

    private fun startTimer() {
        if (timerDisposable == null) {
            timerDisposable =
                Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread()).subscribe {
                        if (duration() == 0) {
                            return@subscribe
                        }
                        progress = getCurrentPos().toFloat() / duration()
                        id()?.let { id ->
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
}
