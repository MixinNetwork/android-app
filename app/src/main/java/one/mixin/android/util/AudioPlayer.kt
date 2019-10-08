package one.mixin.android.util

import android.media.AudioManager
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.RxBus
import one.mixin.android.db.MixinDatabase
import one.mixin.android.event.ProgressEvent
import one.mixin.android.event.RecallEvent
import one.mixin.android.extension.fileExists
import one.mixin.android.extension.openMedia
import one.mixin.android.extension.toast
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.Message
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.isAudio
import one.mixin.android.vo.mediaDownloaded
import one.mixin.android.widget.CircleProgress.Companion.STATUS_DONE
import one.mixin.android.widget.CircleProgress.Companion.STATUS_ERROR
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PAUSE
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY
import timber.log.Timber

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

        fun resume() {
            instance?.resume()
        }

        fun isEnd(): Boolean {
            return instance == null || instance?.status == STATUS_ERROR || instance?.status == STATUS_PAUSE
        }

        fun switchAudioStreamType(useFrontSpeaker: Boolean) {
            instance?.switchAudioStreamType(useFrontSpeaker)
        }

        fun audioFilePlaying(): Boolean {
            return instance?.isFile() == true
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

    fun switchAudioStreamType(useFrontSpeaker: Boolean) {
        if (useFrontSpeaker) {
            player.setAudioStreamType(AudioManager.STREAM_MUSIC)
        } else {
            player.setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
        }
    }

    private val player: MixinPlayer = MixinPlayer(true).also {
        it.setCycle(false)
        it.setOnVideoPlayerListener(object : MixinPlayer.VideoPlayerListenerWrapper() {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    RxBus.publish(ProgressEvent(id!!, 0f, STATUS_PAUSE))
                    stopTimber()
                    status = STATUS_DONE

                    if (autoPlayNext) {
                        checkNext()
                    }
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                if (error.cause is UnrecognizedInputFormatException) {
                    status = STATUS_ERROR
                    RxBus.publish(ProgressEvent(id!!, 0f, STATUS_ERROR))
                    MixinApplication.appContext.toast(R.string.error_not_supported_audio_format)
                    messageItem?.let {
                        MixinApplication.appContext.openMedia(it)
                    }
                } else {
                    status = STATUS_PAUSE
                    RxBus.publish(ProgressEvent(id!!, 0f, STATUS_PAUSE))
                }
                stopTimber()
                it.stop()
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

    private var autoPlayNext: Boolean = true

    private fun isFile(): Boolean {
        return messageItem?.type == MessageCategory.PLAIN_DATA.name || messageItem?.type == MessageCategory.SIGNAL_DATA.name
    }

    interface StatusListener {
        fun onStatusChange(status: Int)
    }

    private var statusListener: StatusListener? = null
    fun setStatusListener(statusListener: StatusListener) {
        this.statusListener = statusListener
    }

    fun play(
        messageItem: MessageItem,
        autoPlayNext: Boolean = true,
        whenPlayNewAudioMessage: ((Message) -> Unit)? = null
    ) {
        this.autoPlayNext = autoPlayNext
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
            player.loadAudio(messageItem.mediaUrl)

            if (autoPlayNext && messageItem.isAudio()) {
                markAudioReadAndCheckNextAudioAvailable(messageItem, whenPlayNewAudioMessage)
            }
        } else if (status == STATUS_DONE || status == STATUS_ERROR) {
            player.loadAudio(messageItem.mediaUrl)
        }
        status = STATUS_PLAY
        player.start()
        if (id != null) {
            RxBus.publish(ProgressEvent(id!!, -1f, STATUS_PLAY))
        }
        startTimer()
    }

    private fun resume() {
        if (messageItem != null && (status == STATUS_PAUSE || status == STATUS_DONE || status == STATUS_ERROR)) {
            play(messageItem!!)
        }
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
        return this.id == id && status != STATUS_ERROR && status != STATUS_DONE
    }

    fun seekTo(progress: Int, max: Float = 100f) {
        val p = progress * player.duration() / max
        player.seekTo(p.toInt())
        RxBus.publish(ProgressEvent(id!!, p, STATUS_PLAY))
    }

    var timerDisposable: Disposable? = null
    var progress = 0f
    private fun startTimer() {
        if (timerDisposable == null) {
            timerDisposable = Observable.interval(0, 100, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread()).subscribe {
                    progress = player.getCurrentPos().toFloat() / player.duration()
                    RxBus.publish(ProgressEvent(id!!, progress, STATUS_PLAY))
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
                currentMessage.conversationId, currentMessage.createdAt, currentMessage.messageId)
                ?: return@launch
            if (message.userId == Session.getAccountId()) return@launch
            if (!mediaDownloaded(message.mediaStatus)) {
                whenPlayNewAction?.invoke(message)
            }
        }
}
