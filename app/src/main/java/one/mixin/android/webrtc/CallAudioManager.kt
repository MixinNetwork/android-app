package one.mixin.android.webrtc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import one.mixin.android.R
import one.mixin.android.extension.isHeadsetOn
import timber.log.Timber

class CallAudioManager(private val context: Context) {
    private val savedSpeakerOn: Boolean
    private val saveMode: Int
    private val savedMicrophoneMute: Boolean

    private var isHeadsetOn = false

    private val audioManager: AudioManager = context.getSystemService<AudioManager>()!!.apply {
        savedSpeakerOn = isSpeakerphoneOn
        saveMode = mode
        savedMicrophoneMute = isMicrophoneMute

        isHeadsetOn = isHeadsetOn()
    }
    private val vibrator: Vibrator? = context.getSystemService()

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

    private val wiredHeadsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val state = intent.getIntExtra("state", STATE_UNPLUGGED)
            val newState = state == STATE_PLUGGED
            if (newState != isHeadsetOn) {
                isHeadsetOn = newState
                updateAudioManager()
            }
        }
    }

    fun start(isInitiator: Boolean) {
        context.registerReceiver(wiredHeadsetReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))

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

        audioManager.isMicrophoneMute = false
        updateAudioManager()
    }

    fun stop() {
        audioManager.mode = if (isHeadsetOn) {
            AudioManager.MODE_NORMAL
        } else {
            AudioManager.MODE_IN_COMMUNICATION
        }
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
        context.unregisterReceiver(wiredHeadsetReceiver)
    }

    private fun updateAudioManager() {
        if (isHeadsetOn) {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } else {
            audioManager.isSpeakerphoneOn = !isInitiator
            if (isInitiator) {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            } else {
                audioManager.mode = AudioManager.MODE_RINGTONE
            }
        }

        mediaPlayer?.reset()
        val audioAttributes = AudioAttributes.Builder()
            .setLegacyStreamType(
                if (isHeadsetOn) {
                    AudioManager.STREAM_MUSIC
                } else {
                    if (isInitiator) {
                        AudioManager.STREAM_VOICE_CALL
                    } else {
                        AudioManager.STREAM_RING
                    }
                }
            )
            .build()
        mediaPlayer?.setAudioAttributes(audioAttributes)

        val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.call}")
        try {
            mediaPlayer?.setDataSource(context, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            Timber.w("mediaPlayer start, $e")
        }
    }

    companion object {
        private const val STATE_UNPLUGGED = 0
        private const val STATE_PLUGGED = 1
    }
}
