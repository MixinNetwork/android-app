package one.mixin.android.webrtc

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import one.mixin.android.R
import one.mixin.android.extension.isBluetoothHeadsetOrWiredHeadset
import one.mixin.android.extension.mainThread
import one.mixin.android.extension.safeActivate
import one.mixin.android.extension.safeStop
import one.mixin.android.extension.selectEarpiece
import one.mixin.android.extension.selectSpeakerphone
import one.mixin.android.extension.startVibration
import one.mixin.android.extension.stopVibration
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
    private var mediaPlayerStopped = false
    private var hasStarted = false

    private var isInitiator = false
    private var playRingtone = false

    private var initAudioSwitch = false

    var isSpeakerOn: Boolean = false
        set(value) {
            Timber.d("$TAG_AUDIO field: $field, value: $value")
            if (field == value) return

            field = value
            setSpeaker(value)
        }

    init {
        initAudioSwitch()
    }

    private fun initAudioSwitch() {
        if (!initAudioSwitch && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)) {
            initAudioSwitch = true
            audioSwitch.start { audioDevices, selectedAudioDevice ->
                Timber.d("$TAG_AUDIO audioDevices: $audioDevices, selectedAudioDevice: $selectedAudioDevice")
                val bluetoothHeadset = audioDevices.find { it is AudioDevice.BluetoothHeadset }
                if (bluetoothHeadset != null && audioSwitch.bluetoothHeadsetManager?.hasActivationError() == false) {
                    audioSwitch.selectDevice(bluetoothHeadset)
                    callback.customAudioDeviceAvailable(true)
                    return@start
                }
                val wiredHeadset = audioDevices.find { it is AudioDevice.WiredHeadset }
                if (wiredHeadset != null) {
                    audioSwitch.selectDevice(wiredHeadset)
                    callback.customAudioDeviceAvailable(true)
                } else {
                    if (mediaPlayerStopped && !isSpeakerOn && selectedAudioDevice !is AudioDevice.Earpiece) {
                        audioSwitch.selectEarpiece()
                    }
                    callback.customAudioDeviceAvailable(false)
                }
            }
        }
    }

    fun start(
        isInitiator: Boolean,
        playRingtone: Boolean = true,
    ) {
        initAudioSwitch()
        mainThread {
            AudioPlayer.pause()
            MusicPlayer.pause()
        }

        hasStarted = true
        this.playRingtone = playRingtone
        this.isInitiator = isInitiator
        val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL

        if (playRingtone && !isInitiator && vibrator != null && ringerMode != AudioManager.RINGER_MODE_SILENT) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(longArrayOf(0, 1000, 1000), 1)
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 1000, 1000), 1)
            }
        }

        setSpeaker(!isInitiator)

        if (playRingtone && (isInitiator || ringerMode == AudioManager.RINGER_MODE_NORMAL)) {
            updateMediaPlayer()
        }
    }

    fun stop() {
        if (mediaPlayerStopped) return

        if (!isInitiator) {
            audioSwitch.selectEarpiece()
        }
        mediaPlayerStopped = true
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        }
        context.stopVibration()
        vibrator?.cancel()
        audioSwitch.safeActivate()
    }

    fun reset() {
        if (!mediaPlayerStopped) {
            stop()
        }

        audioSwitch.deactivate()
        hasStarted = false
        mediaPlayerStopped = false
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
            hasStarted: $hasStarted
            mediaPlayerStopped: $mediaPlayerStopped
            isSpeakerOn: $isSpeakerOn
            audioSwitch selectedAudioDevice: ${audioSwitch.selectedAudioDevice}, availableAudioDevices: ${audioSwitch.availableAudioDevices}
            """.trimIndent()
    }

    private fun setSpeaker(enable: Boolean) {
        val isBluetoothHeadsetOrWiredHeadset = audioSwitch.isBluetoothHeadsetOrWiredHeadset()
        Timber.d("$TAG_AUDIO setSpeaker enable: $enable, isBluetoothHeadsetOrWiredHeadset: $isBluetoothHeadsetOrWiredHeadset")
        if (isBluetoothHeadsetOrWiredHeadset) return

        if (enable) {
            audioSwitch.selectSpeakerphone()
        } else {
            audioSwitch.selectEarpiece()
        }
    }

    private fun updateMediaPlayer() {
        Timber.d("$TAG_AUDIO updateMediaPlayer hasStarted: $hasStarted, mediaPlayerStopped: $mediaPlayerStopped")
        if (!hasStarted || mediaPlayerStopped) return

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        } else {
            mediaPlayer?.reset()
        }
        mediaPlayer?.isLooping = true

        val sound = if (isInitiator) {
            R.raw.call_outgoing
        } else {
            R.raw.call_incoming
        }

        val uri = Uri.parse("android.resource://${context.packageName}/$sound")
        try {
            mediaPlayer?.setDataSource(context, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()

            if (!isInitiator) {
                context.startVibration()
            }
        } catch (e: Exception) {
            Timber.w("$TAG_AUDIO mediaPlayer start, $e")
        }
    }


    interface Callback {
        fun customAudioDeviceAvailable(available: Boolean)
    }
}

const val TAG_AUDIO = "TAG_AUDIO"
