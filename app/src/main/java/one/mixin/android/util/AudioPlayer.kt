package one.mixin.android.util

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import one.mixin.android.RxBus
import one.mixin.android.event.ProgressEvent
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PAUSE
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY
import java.util.concurrent.TimeUnit

class AudioPlayer private constructor() {
    companion object {
        fun get(): AudioPlayer {
            if (instance == null) {
                instance = AudioPlayer()
            }
            return instance!!
        }

        private var instance: AudioPlayer? = null

        fun release() {
            instance?.let {
                it.player.release()
                it.stopTimber()
            }
            instance = null
        }

        fun pause() {
            instance?.pause()
        }
    }

    private val player: MixinPlayer = MixinPlayer(true).also {
        it.setCycle(false)
        it.setOnVideoPlayerListener(object : MixinPlayer.VideoPlayerListenerWrapper() {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    RxBus.publish(ProgressEvent(id!!, 0f, STATUS_PAUSE))
                    stopTimber()
                    status = 0
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                status = STATUS_PAUSE
                stopTimber()
                it.stop()
                RxBus.publish(ProgressEvent(id!!, 0f, STATUS_PAUSE))
            }
        })
    }

    private var id: String? = null
    private var url: String? = null
    private var status = STATUS_PAUSE

    fun play(messageItem: MessageItem) {
        if (id != messageItem.messageId) {
            id = messageItem.messageId
            url = messageItem.mediaUrl
            url?.let {
                player.loadAudio(it)
            }
        } else if (status == 0) {
            player.loadAudio(url!!)
        }
        status = STATUS_PLAY
        player.start()
        if (id != null) {
            RxBus.publish(ProgressEvent(id!!, -1f, STATUS_PLAY))
        }
        startTimer()
    }

    fun pause() {
        status = STATUS_PAUSE
        player.pause()
        if (id != null) {
            RxBus.publish(ProgressEvent(id!!, -1f, STATUS_PAUSE))
        }
        stopTimber()
    }

    fun isPlay(id: String): Boolean {
        return status == STATUS_PLAY && this.id == id
    }

    var timerDisposable: Disposable? = null
    var progress = 0f
    private fun startTimer() {
        if (timerDisposable == null) {
            timerDisposable = Observable.interval(0, 100, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread()).subscribe {
                progress = player.getCurrentPos().toFloat() / player.duration()
                RxBus.publish(ProgressEvent(id!!, progress))
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