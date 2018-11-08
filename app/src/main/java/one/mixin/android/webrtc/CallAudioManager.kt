package one.mixin.android.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import androidx.core.content.getSystemService
import one.mixin.android.R
import timber.log.Timber

class CallAudioManager(context: Context) {
    private val savedSpeakerOn: Boolean
    private val saveMode: Int
    private val savedMicrophoneMute: Boolean
    private val audioManager: AudioManager = context.getSystemService<AudioManager>()!!.apply {
        savedSpeakerOn = isSpeakerphoneOn
        saveMode = mode
        savedMicrophoneMute = isMicrophoneMute
    }

    private var mediaPlayer: MediaPlayer? = MediaPlayer.create(context, R.raw.call).apply {
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
        audioManager.isSpeakerphoneOn = !isInitiator
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.isMicrophoneMute = false
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

    fun release() {
        audioManager.isSpeakerphoneOn = savedSpeakerOn
        audioManager.mode = saveMode
        audioManager.isMicrophoneMute = savedMicrophoneMute
    }
}