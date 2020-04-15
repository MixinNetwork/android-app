package one.mixin.android.webrtc

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import one.mixin.android.R
import timber.log.Timber

class CallAudioManager(private val context: Context) {
    private val savedSpeakerOn: Boolean
    private val saveMode: Int
    private val savedMicrophoneMute: Boolean
    private val audioManager: AudioManager = context.getSystemService<AudioManager>()!!.apply {
        savedSpeakerOn = isSpeakerphoneOn
        saveMode = mode
        savedMicrophoneMute = isMicrophoneMute
    }
    private val vibrator: Vibrator? = context.getSystemService<Vibrator>()

    private var mediaPlayer: MediaPlayer? = MediaPlayer().apply {
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

        if (!isInitiator && vibrator != null && audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 1)
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 1000), 1)
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setLegacyStreamType(
                if (isInitiator) AudioManager.STREAM_VOICE_CALL else AudioManager.STREAM_RING)
            .build()
        if (isInitiator) {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        } else {
            audioManager.mode = AudioManager.MODE_RINGTONE
        }
        mediaPlayer?.setAudioAttributes(audioAttributes)
        audioManager.isSpeakerphoneOn = !isInitiator
        audioManager.isMicrophoneMute = false
        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.call}")
        try {
            mediaPlayer?.setDataSource(context, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            Timber.w("mediaPlayer start, $e")
        }
    }

    fun stop() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        if (mediaPlayer != null) {
            mediaPlayer?.release()
            mediaPlayer = null
        }
        if (!isInitiator && !changedByUser) {
            audioManager.isSpeakerphoneOn = false
        }
        vibrator?.cancel()
    }

    fun release() {
        audioManager.isSpeakerphoneOn = savedSpeakerOn
        audioManager.mode = saveMode
        audioManager.isMicrophoneMute = savedMicrophoneMute
        vibrator?.cancel()
    }
}
