package one.mixin.android.util

import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Player
import one.mixin.android.RxBus
import one.mixin.android.event.ProgressEvent
import one.mixin.android.util.video.MixinPlayer
import one.mixin.android.vo.MessageItem
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PAUSE
import one.mixin.android.widget.CircleProgress.Companion.STATUS_PLAY

class AudioPlay private constructor() {
    companion object {
        var isInit = false
        val instance: AudioPlay by lazy {
            isInit = true
            AudioPlay()
        }
    }

    private val player: MixinPlayer = MixinPlayer(true).also {
        it.setCycle(false)
        it.setOnVideoPlayerListener(object : MixinPlayer.VideoPlayerListenerWrapper() {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                status = if (playbackState == Player.STATE_ENDED) {
                    RxBus.getInstance().post(ProgressEvent(id!!, 0f, STATUS_PAUSE))
                    STATUS_PAUSE
                } else {
                    STATUS_PLAY
                }
            }

            override fun onPlayerError(error: ExoPlaybackException) {
                status = STATUS_PAUSE
                RxBus.getInstance().post(ProgressEvent(id!!, 0f, STATUS_PAUSE))
            }
        })
    }

    var id: String? = null
    private var url: String? = null
    private var status = STATUS_PAUSE

    fun play(messageItem: MessageItem) {
        if (id != messageItem.messageId) {
            id = messageItem.messageId
            url = messageItem.mediaUrl
            player.loadAudio(messageItem.mediaUrl!!)
        }
        status = STATUS_PLAY
        player.seekTo(0)
        player.start()
        if (id != null) {
            RxBus.getInstance().post(ProgressEvent(id!!, 0f, STATUS_PLAY))
        }
    }

    fun stop() {
        status = STATUS_PAUSE
        player.stop()
        if (id != null) {
            RxBus.getInstance().post(ProgressEvent(id!!, 0f, STATUS_PAUSE))
        }
    }

    fun isPlay(id: String): Boolean {
        return status == STATUS_PLAY && this.id == id
    }
}