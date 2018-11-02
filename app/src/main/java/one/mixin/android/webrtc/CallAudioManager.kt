package one.mixin.android.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.core.content.getSystemService
import one.mixin.android.R
import timber.log.Timber

class CallAudioManager(context: Context) {
    private val audioManager: AudioManager = context.getSystemService<AudioManager>()!!

    private var mediaPlayer: MediaPlayer? = MediaPlayer.create(context, R.raw.call_ring).apply {
        isLooping = true
    }

    var isSpeakerOn = false
        set(value) {
            if (value == field) {
                return
            }
            changedByUser = true
            field = value
            audioManager.isSpeakerphoneOn = value
        }

    private var isInitiator = false
    private var changedByUser = false

    fun start(isInitiator: Boolean) {
        this.isInitiator = isInitiator
        val audioAttributes = AudioAttributes.Builder()
            .setLegacyStreamType(
                if (isInitiator) AudioManager.STREAM_VOICE_CALL else AudioManager.STREAM_MUSIC)
            .build()
        mediaPlayer?.setAudioAttributes(audioAttributes)
        if (isInitiator) {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        } else {
            audioManager.isSpeakerphoneOn = true
        }
        try {
            mediaPlayer?.start()
        } catch (e: Exception) {
            Timber.w("mediaPlayer start, $e")
        }
    }

    fun stop() {
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
        if (!isInitiator && !changedByUser) {
            audioManager.isSpeakerphoneOn = false
        }
    }
}