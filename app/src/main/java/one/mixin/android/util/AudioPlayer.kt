package one.mixin.android.util

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.RxBus
import one.mixin.android.db.MixinDatabase
import one.mixin.android.event.ProgressEvent
import one.mixin.android.event.RecallEvent
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isAudio
import one.mixin.android.widget.CircleProgress.Companion.STATUS_ERROR
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PAUSE
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY
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

        fun release() {
            instance?.let {
                it.recallDisposable?.let { disposable ->
                    if (!disposable.isDisposed) {
                        disposable.dispose()
                    }
                }
                it.player.release()
                it.stopTimber()
            }
            instance = null
        }

        fun pause() {
            instance?.pause()
        }

        fun isEnd(): Boolean {
            return instance?.status == STATUS_PAUSE || instance?.status == STATUS_ERROR
        }
    }

    private var recallDisposable: Disposable? = null

    init {
        recallDisposable = RxBus.listen(RecallEvent::class.java)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                Timber.d("recall:" + it.messageId)
                if (id == it.messageId) {
                    pause()
                }
            }
    }

    private val player: MixinPlayer = MixinPlayer(true).also {
        it.setCycle(false)
        it.setOnVideoPlayerListener(object : MixinPlayer.VideoPlayerListenerWrapper() {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    RxBus.publish(ProgressEvent(id!!, 0f, STATUS_PAUSE))
                    stopTimber()
                    status = STATUS_ERROR

                    checkNext()
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
    private var messageItem: MessageItem? = null
    private var status = STATUS_PAUSE
        set(value) {
            if (field != value) {
                field = value
                statusListener?.onStatusChange(value)
            }
        }

    interface StatusListener {
        fun onStatusChange(status: Int)
    }

    private var statusListener: StatusListener? = null
    fun setStatusListener(statusListener: StatusListener) {
        this.statusListener = statusListener
    }

    fun play(messageItem: MessageItem) {
        if (id != messageItem.messageId) {
            id = messageItem.messageId
            this.messageItem = messageItem
            messageItem.mediaUrl?.let {
                player.loadAudio(it)
            }
        } else if (status == STATUS_ERROR) {
            player.loadAudio(messageItem.mediaUrl!!)
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

    fun isLoaded(id: String): Boolean {
        return this.id == id && status != STATUS_ERROR
    }

    var timerDisposable: Disposable? = null
    var progress = 0f
    private fun startTimer() {
        if (timerDisposable == null) {
            timerDisposable = Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread()).subscribe {
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

    private fun checkNext() {
        messageItem?.let { item ->
            GlobalScope.launch(Dispatchers.IO) {
                val nextMessage = MixinDatabase.getDatabase(MixinApplication.appContext)
                    .messageDao()
                    .findNextAudioMessageItem(item.conversationId, item.createdAt, item.messageId) ?: return@launch
                if (nextMessage.userId != item.userId || !nextMessage.isAudio() || nextMessage.mediaUrl == null) return@launch

                withContext(Dispatchers.Main) {
                    play(nextMessage)
                }
            }
        }
    }
}