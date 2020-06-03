package one.mixin.android.webrtc

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import one.mixin.android.R
import one.mixin.android.extension.isValidAudioDeviceTypeOut
import timber.log.Timber

class CallAudioManager(private val context: Context) {
    private val savedSpeakerOn: Boolean
    private val saveMode: Int
    private val savedMicrophoneMute: Boolean

    private var audioDevices = mutableSetOf<Int>()
    private var userSelectedAudioDevice = AudioDeviceInfo.TYPE_UNKNOWN
    private var selectedAudioDevice = AudioDeviceInfo.TYPE_UNKNOWN
    private var defaultAudioDevice = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
    private var bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private var hasWiredHeadset = false
    private var bluetoothState = State.UNINITIALIZED
    private var bluetoothHeadset: BluetoothHeadset? = null

    private val audioManager: AudioManager = context.getSystemService<AudioManager>()!!.apply {
        savedSpeakerOn = isSpeakerphoneOn
        saveMode = mode
        savedMicrophoneMute = isMicrophoneMute
    }
    private val vibrator: Vibrator? = context.getSystemService()

    private var mediaPlayer: MediaPlayer? = null
    private var mediaPlayerStopped = false
    private var hasStarted = false

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
            hasWiredHeadset = state == STATE_PLUGGED
            updateAudioDevice()
        }
    }

    private val bluetoothHeadsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (bluetoothState == State.UNINITIALIZED) return

            val action = intent?.action
            if (action == BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED)
                if (state == BluetoothHeadset.STATE_CONNECTED) {
                    bluetoothState = State.SCO_CONNECTED
                    updateAudioDevice()
                } else if (state == BluetoothHeadset.STATE_DISCONNECTED) {
                    stopScoAudio()
                    updateAudioDevice()
                }
            } else if (action == BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_AUDIO_DISCONNECTED)
                if (state == BluetoothHeadset.STATE_AUDIO_CONNECTED) {
                    if (bluetoothState == State.SCO_CONNECTING) {
                        bluetoothState = State.SCO_CONNECTED
                        updateAudioDevice()
                    } else if (state == BluetoothHeadset.STATE_AUDIO_DISCONNECTED) {
                        updateAudioDevice()
                    }
                }
            }
        }
    }

    private val bluetoothServiceListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceDisconnected(profile: Int) {
            stopScoAudio()
            bluetoothHeadset = null
            updateAudioDevice()
        }

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile?) {
            if (profile != BluetoothProfile.HEADSET || bluetoothState == State.UNINITIALIZED) return

            bluetoothState = State.HEADSET_UNAVAILABLE
            bluetoothHeadset = proxy as? BluetoothHeadset
            updateAudioDevice()
        }
    }

    fun start(isInitiator: Boolean) {
        hasStarted = true
        context.registerReceiver(wiredHeadsetReceiver, IntentFilter(Intent.ACTION_HEADSET_PLUG))
        context.registerReceiver(
            bluetoothHeadsetReceiver,
            IntentFilter().apply {
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
            }
        )
        bluetoothState = State.HEADSET_UNAVAILABLE
        defaultAudioDevice = if (isInitiator) {
            AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        } else {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
        bluetoothAdapter.getProfileProxy(context, bluetoothServiceListener, BluetoothProfile.HEADSET)

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
        updateAudioDevice()
    }

    fun stop() {
        if (mediaPlayerStopped) return

        mediaPlayerStopped = true
        audioManager.mode = if (bluetoothState == State.SCO_CONNECTED) {
            AudioManager.MODE_NORMAL
        } else AudioManager.MODE_IN_COMMUNICATION
        if (mediaPlayer != null) {
            mediaPlayer?.stop()
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
        stopScoAudio()
        if (bluetoothHeadset != null) {
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothHeadset)
            bluetoothHeadset = null
        }
        if (hasStarted) {
            context.unregisterReceiver(wiredHeadsetReceiver)
            context.unregisterReceiver(bluetoothHeadsetReceiver)
        }
        bluetoothState = State.UNINITIALIZED
    }

    @Synchronized
    private fun updateMediaPlayer() {
        if (mediaPlayerStopped) return

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer()
        } else {
            mediaPlayer?.reset()
        }
        mediaPlayer?.isLooping = true
        val audioAttributes = AudioAttributes.Builder()
            .setLegacyStreamType(
                if (bluetoothState == State.SCO_CONNECTED) {
                    AudioManager.STREAM_VOICE_CALL
                } else if (hasWiredHeadset) {
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

        val sound = if (isInitiator) {
            R.raw.call_outgoing
        } else R.raw.call_incoming
        val uri = Uri.parse("android.resource://${context.packageName}/$sound")
        try {
            mediaPlayer?.setDataSource(context, uri)
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            Timber.w("mediaPlayer start, $e")
        }
    }

    private fun updateAudioDevice() {
        if (bluetoothState == State.HEADSET_AVAILABLE ||
            bluetoothState == State.HEADSET_UNAVAILABLE ||
            bluetoothState == State.SCO_CONNECTING
        ) {
            updateBluetoothDevice()
        }

        val newAudioDevices = mutableSetOf<Int>()

        if (bluetoothState == State.HEADSET_AVAILABLE ||
            bluetoothState == State.SCO_CONNECTED ||
            bluetoothState == State.SCO_CONNECTING
        ) {
            newAudioDevices.add(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        }
        if (hasWiredHeadset) {
            newAudioDevices.add(AudioDeviceInfo.TYPE_WIRED_HEADSET)
        } else {
            newAudioDevices.add(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)
            if (hasEarpiece()) {
                newAudioDevices.add(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE)
            }
        }
        var audioDeviceSetUpdated = audioDevices != newAudioDevices
        audioDevices = newAudioDevices
        if (bluetoothState == State.HEADSET_UNAVAILABLE &&
            userSelectedAudioDevice == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        ) {
            userSelectedAudioDevice = AudioDeviceInfo.TYPE_UNKNOWN
        }
        if (hasWiredHeadset && userSelectedAudioDevice == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
            userSelectedAudioDevice = AudioDeviceInfo.TYPE_WIRED_HEADSET
        }
        if (!hasWiredHeadset && userSelectedAudioDevice == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
            userSelectedAudioDevice = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }

        val needBluetoothAudioStart = bluetoothState == State.HEADSET_AVAILABLE &&
            (
                userSelectedAudioDevice == AudioDeviceInfo.TYPE_UNKNOWN ||
                    userSelectedAudioDevice == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                )
        val needBluetoothAudioStop = (
            bluetoothState == State.SCO_CONNECTED ||
                bluetoothState == State.SCO_CONNECTING
            ) &&
            (
                userSelectedAudioDevice != AudioDeviceInfo.TYPE_UNKNOWN &&
                    userSelectedAudioDevice != AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                )
        if (needBluetoothAudioStop) {
            stopScoAudio()
            updateBluetoothDevice()
        }
        if (needBluetoothAudioStart && !needBluetoothAudioStop) {
            if (!startScoAudio()) {
                audioDevices.remove(AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
                audioDeviceSetUpdated = true
            }
        }
        val newAudioDevice = when {
            bluetoothState == State.SCO_CONNECTED -> {
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
            hasWiredHeadset -> {
                AudioDeviceInfo.TYPE_WIRED_HEADSET
            }
            else -> {
                defaultAudioDevice
            }
        }
        if (newAudioDevice != selectedAudioDevice || audioDeviceSetUpdated) {
            if (newAudioDevice != selectedAudioDevice) {
                updateMediaPlayer()
            }
            setAudioDeviceInternal(newAudioDevice)
        }
    }

    private fun setAudioDeviceInternal(device: Int) {
        require(isValidAudioDeviceTypeOut(device))

        audioManager.isSpeakerphoneOn = when (device) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> true
            else -> false
        }
        selectedAudioDevice = device
    }

    private fun updateBluetoothDevice() {
        if (bluetoothState == State.UNINITIALIZED || bluetoothHeadset == null) return

        val connectedDevices = bluetoothHeadset?.connectedDevices
        bluetoothState = if (connectedDevices.isNullOrEmpty()) {
            State.HEADSET_UNAVAILABLE
        } else {
            State.HEADSET_AVAILABLE
        }
    }

    private fun startScoAudio(): Boolean {
        if (bluetoothState != State.HEADSET_AVAILABLE) return false

        bluetoothState = State.SCO_CONNECTING
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
        return true
    }

    private fun stopScoAudio() {
        if (bluetoothState != State.SCO_CONNECTED && bluetoothState != State.SCO_CONNECTING) return

        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        bluetoothState = State.SCO_DISCONNECTING
    }

    private fun hasEarpiece() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)

    enum class State {
        UNINITIALIZED,
        ERROR,
        HEADSET_UNAVAILABLE,
        HEADSET_AVAILABLE,
        SCO_DISCONNECTING,
        SCO_CONNECTING,
        SCO_CONNECTED
    }

    companion object {
        private const val STATE_UNPLUGGED = 0
        private const val STATE_PLUGGED = 1
    }
}
