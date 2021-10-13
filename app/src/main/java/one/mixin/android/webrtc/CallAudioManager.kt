package one.mixin.android.webrtc

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import one.mixin.android.R
import one.mixin.android.extension.isBluetoothHeadsetOrWiredHeadset
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.safeActivate
import one.mixin.android.extension.safeStop
import one.mixin.android.extension.selectEarpieceAndActivate
import one.mixin.android.extension.selectSpeakerphone
import one.mixin.android.util.AudioPlayer
import one.mixin.android.util.MusicPlayer
import timber.log.Timber

class CallAudioManager(
    private val context: Context,
    private val audioSwitch: AudioSwitch,
    private val callback: Callback,
) {

    private val audioManager: AudioManager? = context.getSystemService()
    private val vibrator: Vibrator? = context.getSystemService()

    private var mediaPlayer: MediaPlayer? = null
    private var activated = false

    private var isInitiator = false
    private var playRingtone = false

    var isSpeakerOn: Boolean = false
        set(value) {
            Timber.d("$TAG_AUDIO field: $field, value: $value")
            if (field == value) return

            field = value
            setSpeaker(value)
        }

    fun start(isInitiator: Boolean, playRingtone: Boolean = true) {
        context.mainThread {
            AudioPlayer.pause()
            MusicPlayer.pause()
        }

        audioSwitch.start { audioDevices, selectedAudioDevice ->
            var targetDevice = audioDevices[0]
            Timber.d("$TAG_AUDIO audioDevices: $audioDevices, selectedAudioDevice: $selectedAudioDevice, targetDevice: $targetDevice")
            if (targetDevice != selectedAudioDevice || (isSpeakerOn && targetDevice is AudioDevice.Earpiece) || (!isSpeakerOn && targetDevice is AudioDevice.Speakerphone)) {
                if (targetDevice is AudioDevice.BluetoothHeadset || targetDevice is AudioDevice.WiredHeadset) {
                    audioSwitch.deactivate()
                    audioSwitch.selectDevice(targetDevice)
                    if (activated) {
                        audioSwitch.safeActivate()
                    }
                } else {
                    if (isSpeakerOn) {
                        audioDevices.find { it is AudioDevice.Speakerphone }
                            ?.let {
                                audioSwitch.deactivate()
                                audioSwitch.selectDevice(it)
                                if (activated) {
                                    audioSwitch.safeActivate()
                                }
                                targetDevice = it
                            }
                    } else {
                        audioDevices.find { it is AudioDevice.Earpiece }
                            ?.let {
                                audioSwitch.selectDevice(it)
                                audioSwitch.safeActivate()
                                targetDevice = it
                            }
                    }
                }
            }
            callback.customAudioDeviceAvailable(targetDevice.isBluetoothHeadsetOrWiredHeadset())
        }

        this.playRingtone = playRingtone
        this.isInitiator = isInitiator

        if (playRingtone && !isInitiator && vibrator != null && audioManager?.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 1)
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 1000), 1)
            }
        }

        setSpeaker(!isInitiator)

        if (playRingtone) {
            updateMediaPlayer()
        }
    }

    fun stop() {
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
        vibrator?.cancel()
    }

    fun active() {
        audioSwitch.safeActivate()
        activated = true
    }

    fun reset() {
        stop()
        audioSwitch.deactivate()
        activated = false
        isInitiator = false
        isSpeakerOn = false
        playRingtone = false
    }

    fun release() {
        audioSwitch.safeStop()
    }

    fun getMsg(): String {
        return """
            $TAG_AUDIO CallAudioManager message
            isInitiator: $isInitiator
            playRingtone: $playRingtone
            isSpeakerOn: $isSpeakerOn
            audioSwitch selectedAudioDevice: ${audioSwitch.selectedAudioDevice}, availableAudioDevices: ${audioSwitch.availableAudioDevices}
        """.trimIndent()
    }

    private fun setSpeaker(enable: Boolean) {
        val isBluetoothHeadsetOrWiredHeadset = audioSwitch.isBluetoothHeadsetOrWiredHeadset()
        Timber.d("$TAG_AUDIO setSpeaker enable: $enable, isBluetoothHeadsetOrWiredHeadset: $isBluetoothHeadsetOrWiredHeadset")
        if (isBluetoothHeadsetOrWiredHeadset) return

        if (enable) {
            audioSwitch.deactivate()
            audioSwitch.selectSpeakerphone()
            if (activated) {
                audioSwitch.safeActivate()
            }
        } else {
            audioSwitch.selectEarpieceAndActivate()
        }
    }

    private fun updateMediaPlayer() {
        Timber.d("$TAG_AUDIO updateMediaPlayer")

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        } else {
            mediaPlayer?.reset()
        }
        mediaPlayer?.isLooping = true

        val sound = if (isInitiator) {
            R.raw.call_outgoing
        } else R.raw.call_incoming
        val uri = Uri.parse("android.resource://${context.packageName}/$sound")
        try {
            mediaPlayer?.setDataSource(context, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            Timber.w("$TAG_AUDIO mediaPlayer start, $e")
        }
    }

    interface Callback {
        fun customAudioDeviceAvailable(available: Boolean)
    }
}

const val TAG_AUDIO = "TAG_AUDIO"
