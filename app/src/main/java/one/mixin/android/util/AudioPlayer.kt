package one.mixin.android.util

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import one.mixin.android.RxBus
import one.mixin.android.event.ProgressEvent
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PAUSE
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY

class AudioPlayer private constructor() {
    companion object {
        var isInit = false
        val instance: AudioPlayer by lazy {
            isInit = true
            AudioPlayer()
        }
    }

    private val player: MixinPlayer = MixinPlayer(true).also {
        it.setCycle(false)
        it.setOnVideoPlayerListener(object : MixinPlayer.VideoPlayerListenerWrapper() {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    RxBus.publish(ProgressEvent(id!!, 0f, STATUS_PAUSE))
                    status = STATUS_PAUSE
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                status = STATUS_PAUSE
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
        }
        status = STATUS_PLAY
        url?.let {
            player.loadAudio(it)
            player.start()
        }
        if (id != null) {
            RxBus.publish(ProgressEvent(id!!, 0f, STATUS_PLAY))
        }
    }

    fun stop() {
        status = STATUS_PAUSE
        player.stop()
        if (id != null) {
            RxBus.publish(ProgressEvent(id!!, 0f, STATUS_PAUSE))
        }
    }

    fun isPlay(id: String): Boolean {
        return status == STATUS_PLAY && this.id == id
    }
}