package one.mixin.android.extension

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import one.mixin.android.util.reportException
import one.mixin.android.webrtc.TAG_AUDIO
import timber.log.Timber

sealed class AudioDevice {
    abstract val name: String

    data class BluetoothHeadset(
        override val name: String = "Bluetooth",
    ) : AudioDevice()

    data class WiredHeadset(
        override val name: String = "Wired Headset",
    ) : AudioDevice()

    data class Earpiece(
        override val name: String = "Earpiece",
    ) : AudioDevice()

    data class Speakerphone(
        override val name: String = "Speakerphone",
    ) : AudioDevice()
}

class AudioSwitch(
    context: Context,
    loggingEnabled: Boolean = false,
    preferredDeviceList: List<Class<out AudioDevice>> = defaultPreferredDeviceList,
) {
    private val context = context.applicationContext
    private val audioManager =
        requireNotNull(this.context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
    private val handler = Handler(Looper.getMainLooper())
    private val lock = Any()
    private val preferredDeviceClasses = buildPreferredDeviceList(preferredDeviceList)
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
        log("Audio focus changed: $it")
    }

    private var state = State.STOPPED
    private var audioDeviceChangeListener: ((List<AudioDevice>, AudioDevice?) -> Unit)? = null
    private var userSelectedDevice: AudioDevice? = null
    private var wiredHeadsetAvailable = false
    private var bluetoothHeadsetProxy: BluetoothHeadset? = null
    private var bluetoothActivationError = false
    private var bluetoothScoActive = false
    private var bluetoothScoConnecting = false
    private var bluetoothScoTimeoutRunnable: Runnable? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var receiverRegistered = false
    private var audioDeviceCallbackRegistered = false

    private var savedAudioMode = AudioManager.MODE_NORMAL
    private var savedIsMicrophoneMuted = false
    private var savedSpeakerphoneEnabled = false
    private var savedCommunicationDevice: AudioDeviceInfo? = null
    private var hasSavedAudioState = false

    @Volatile
    private var devices: List<AudioDevice> = emptyList()

    @Volatile
    private var selectedDevice: AudioDevice? = null

    @Volatile
    var loggingEnabled: Boolean = loggingEnabled

    val selectedAudioDevice: AudioDevice?
        get() = selectedDevice

    val availableAudioDevices: List<AudioDevice>
        get() = devices

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
            refreshDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
            refreshDevices()
        }
    }

    private val bluetoothProfileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(
            profile: Int,
            proxy: BluetoothProfile,
        ) {
            if (profile == BluetoothProfile.HEADSET) {
                synchronized(lock) {
                    bluetoothHeadsetProxy = proxy as? BluetoothHeadset
                }
                refreshDevices()
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                synchronized(lock) {
                    bluetoothHeadsetProxy = null
                }
                refreshDevices()
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        @Suppress("DEPRECATION")
        override fun onReceive(
            context: Context,
            intent: Intent,
        ) {
            when (intent.action) {
                Intent.ACTION_HEADSET_PLUG,
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
                BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED,
                -> refreshDevices()

                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_ON) {
                        startBluetoothProfile()
                    }
                    refreshDevices()
                }

                AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED -> {
                    when (intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR)) {
                        AudioManager.SCO_AUDIO_STATE_CONNECTED -> onBluetoothScoConnected()
                        AudioManager.SCO_AUDIO_STATE_DISCONNECTED -> onBluetoothScoDisconnected()
                    }
                }
            }
        }
    }

    init {
        require(preferredDeviceList.distinct().size == preferredDeviceList.size) {
            "preferredDeviceList contains duplicate audio devices"
        }
    }

    fun start(listener: ((List<AudioDevice>, AudioDevice?) -> Unit)) {
        var notifyImmediately = false
        var currentDevices: List<AudioDevice> = emptyList()
        var currentSelectedDevice: AudioDevice? = null
        synchronized(lock) {
            audioDeviceChangeListener = listener
            if (state != State.STOPPED) {
                notifyImmediately = true
                currentDevices = devices
                currentSelectedDevice = selectedDevice
            }
        }
        if (notifyImmediately) {
            listener.invoke(currentDevices, currentSelectedDevice)
        } else {
            start()
        }
    }

    fun start() {
        synchronized(lock) {
            if (state != State.STOPPED) return
            state = State.STARTED
            wiredHeadsetAvailable = hasWiredHeadset()
        }

        registerAudioDeviceCallback()
        registerReceiver()
        startBluetoothProfile()
        refreshDevices()
    }

    fun stop() {
        val bluetoothProxy: BluetoothHeadset?
        synchronized(lock) {
            if (state == State.STOPPED) return
            if (state == State.ACTIVATED) {
                restoreAudioStateLocked()
            }
            cancelBluetoothScoTimeoutLocked()
            state = State.STOPPED
            devices = emptyList()
            selectedDevice = null
            userSelectedDevice = null
            bluetoothActivationError = false
            bluetoothScoActive = false
            bluetoothScoConnecting = false
            audioDeviceChangeListener = null
            bluetoothProxy = bluetoothHeadsetProxy
            bluetoothHeadsetProxy = null
        }

        unregisterAudioDeviceCallback()
        unregisterReceiver()
        if (bluetoothProxy != null) {
            runCatching {
                BluetoothAdapter.getDefaultAdapter()?.closeProfileProxy(
                    BluetoothProfile.HEADSET,
                    bluetoothProxy,
                )
            }.onFailure { log("Close Bluetooth profile failed: $it") }
        }
    }

    fun activate() {
        val device: AudioDevice?
        synchronized(lock) {
            when (state) {
                State.STARTED -> {
                    cacheAudioStateLocked()
                    requestAudioFocusLocked()
                    state = State.ACTIVATED
                }

                State.ACTIVATED -> Unit
                State.STOPPED -> throw IllegalStateException("AudioSwitch is stopped")
            }
            device = selectedDevice
        }

        if (device != null && !routeDevice(device)) {
            markBluetoothActivationError()
            refreshDevices()
        }
    }

    fun deactivate() {
        synchronized(lock) {
            if (state != State.ACTIVATED) return
            restoreAudioStateLocked()
            state = State.STARTED
        }
    }

    fun selectDevice(audioDevice: AudioDevice?) {
        var deviceToRoute: AudioDevice? = null
        var shouldNotify = false
        synchronized(lock) {
            if (state == State.STOPPED) return
            val requestedDevice = resolveAvailableDeviceLocked(audioDevice) ?: return
            val shouldRetryBluetooth = requestedDevice is AudioDevice.BluetoothHeadset &&
                bluetoothActivationError
            if (requestedDevice is AudioDevice.BluetoothHeadset) {
                bluetoothActivationError = false
            }
            userSelectedDevice = requestedDevice
            if (selectedDevice != requestedDevice) {
                selectedDevice = requestedDevice
                shouldNotify = true
                if (state == State.ACTIVATED) {
                    deviceToRoute = requestedDevice
                }
            } else if (shouldRetryBluetooth && state == State.ACTIVATED) {
                deviceToRoute = requestedDevice
            }
        }

        val selectedDeviceToRoute = deviceToRoute
        if (selectedDeviceToRoute != null && !routeDevice(selectedDeviceToRoute)) {
            markBluetoothActivationError()
            refreshDevices()
            return
        }
        if (shouldNotify) {
            notifyAudioDeviceChanged()
        }
    }

    fun hasBluetoothActivationError(): Boolean = synchronized(lock) {
        bluetoothActivationError
    }

    private fun refreshDevices() {
        var deviceToRoute: AudioDevice? = null
        var shouldNotify = false
        synchronized(lock) {
            if (state == State.STOPPED) return

            val oldDevices = devices
            val oldSelectedDevice = selectedDevice
            wiredHeadsetAvailable = hasWiredHeadset()
            val newDevices = buildAvailableDevicesLocked()
            if (newDevices.none { it is AudioDevice.BluetoothHeadset }) {
                bluetoothActivationError = false
            }

            val resolvedUserDevice = resolveUserSelectedDeviceLocked(newDevices)
            if (resolvedUserDevice == null) {
                userSelectedDevice = null
            } else {
                userSelectedDevice = resolvedUserDevice
            }
            val newSelectedDevice = resolvedUserDevice ?: newDevices.firstOrNull {
                !(it is AudioDevice.BluetoothHeadset && bluetoothActivationError)
            }

            devices = newDevices.toList()
            selectedDevice = newSelectedDevice
            shouldNotify = oldDevices != devices || oldSelectedDevice != selectedDevice
            if (state == State.ACTIVATED && oldSelectedDevice != selectedDevice) {
                deviceToRoute = selectedDevice
            }
        }

        val selectedDeviceToRoute = deviceToRoute
        if (selectedDeviceToRoute != null && !routeDevice(selectedDeviceToRoute)) {
            markBluetoothActivationError()
            refreshDevices()
            return
        }
        if (shouldNotify) {
            notifyAudioDeviceChanged()
        }
    }

    private fun notifyAudioDeviceChanged() {
        val listener: ((List<AudioDevice>, AudioDevice?) -> Unit)
        val currentDevices: List<AudioDevice>
        val currentSelectedDevice: AudioDevice?
        synchronized(lock) {
            listener = audioDeviceChangeListener ?: return
            currentDevices = devices
            currentSelectedDevice = selectedDevice
        }
        listener.invoke(currentDevices, currentSelectedDevice)
    }

    private fun buildAvailableDevicesLocked(): List<AudioDevice> {
        val bluetoothHeadset = bluetoothHeadsetDeviceLocked()
        val result = ArrayList<AudioDevice>(preferredDeviceClasses.size)
        preferredDeviceClasses.forEach { deviceClass ->
            when (deviceClass) {
                AudioDevice.BluetoothHeadset::class.java -> bluetoothHeadset?.let(result::add)
                AudioDevice.WiredHeadset::class.java -> {
                    if (wiredHeadsetAvailable) result += AudioDevice.WiredHeadset()
                }

                AudioDevice.Earpiece::class.java -> {
                    if (hasEarpiece() && !wiredHeadsetAvailable) result += AudioDevice.Earpiece()
                }

                AudioDevice.Speakerphone::class.java -> {
                    if (hasSpeakerphone()) result += AudioDevice.Speakerphone()
                }
            }
        }
        log("Available audio devices: $result")
        return result
    }

    private fun resolveAvailableDeviceLocked(audioDevice: AudioDevice?): AudioDevice? {
        if (audioDevice == null) return devices.firstOrNull()
        return if (audioDevice is AudioDevice.BluetoothHeadset) {
            devices.firstOrNull { it is AudioDevice.BluetoothHeadset }
        } else {
            devices.firstOrNull { it == audioDevice }
        }
    }

    private fun resolveUserSelectedDeviceLocked(newDevices: List<AudioDevice>): AudioDevice? {
        val selected = userSelectedDevice ?: return null
        return if (selected is AudioDevice.BluetoothHeadset) {
            newDevices.firstOrNull { it is AudioDevice.BluetoothHeadset }
        } else {
            newDevices.firstOrNull { it == selected }
        }
    }

    private fun buildPreferredDeviceList(
        preferredDeviceList: List<Class<out AudioDevice>>,
    ): List<Class<out AudioDevice>> {
        val defaultList = defaultPreferredDeviceList
        if (preferredDeviceList.isEmpty() || preferredDeviceList == defaultList) return defaultList
        val result = defaultList.toMutableList()
        preferredDeviceList.forEach { result.remove(it) }
        preferredDeviceList.forEachIndexed { index, deviceClass -> result.add(index, deviceClass) }
        return result
    }

    private fun registerAudioDeviceCallback() {
        synchronized(lock) {
            if (audioDeviceCallbackRegistered) return
            runCatching {
                audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler)
                audioDeviceCallbackRegistered = true
            }.onFailure { log("Register audio device callback failed: $it") }
        }
    }

    private fun unregisterAudioDeviceCallback() {
        synchronized(lock) {
            if (!audioDeviceCallbackRegistered) return
            runCatching { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback) }
                .onFailure { log("Unregister audio device callback failed: $it") }
            audioDeviceCallbackRegistered = false
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerReceiver() {
        synchronized(lock) {
            if (receiverRegistered) return
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_HEADSET_PLUG)
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
                }
            }
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    context.registerReceiver(receiver, filter)
                }
                receiverRegistered = true
            }.onFailure { log("Register audio receiver failed: $it") }
        }
    }

    private fun unregisterReceiver() {
        synchronized(lock) {
            if (!receiverRegistered) return
            runCatching { context.unregisterReceiver(receiver) }
                .onFailure { log("Unregister audio receiver failed: $it") }
            receiverRegistered = false
        }
    }

    @Suppress("DEPRECATION")
    private fun startBluetoothProfile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        val adapter = runCatching { BluetoothAdapter.getDefaultAdapter() }.getOrNull() ?: return
        runCatching {
            if (adapter.isEnabled) {
                adapter.getProfileProxy(context, bluetoothProfileListener, BluetoothProfile.HEADSET)
            }
        }.onFailure { log("Start Bluetooth profile failed: $it") }
    }

    private fun hasWiredHeadset(): Boolean {
        return audioOutputDevices().any { it.isWiredHeadset() }
    }

    private fun hasEarpiece(): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY) ||
            audioOutputDevices().any { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
    }

    private fun hasSpeakerphone(): Boolean {
        return audioOutputDevices().any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } ||
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)
    }

    private fun bluetoothHeadsetDeviceLocked(): AudioDevice.BluetoothHeadset? {
        val name = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            communicationDevices().firstOrNull { it.isBluetoothHeadset() }?.productName?.toString()
        } else {
            bluetoothHeadsetProxy?.let { proxy ->
                runCatching {
                    val device = proxy.connectedDevices.firstOrNull { proxy.isAudioConnected(it) }
                        ?: proxy.connectedDevices.firstOrNull()
                    device?.name
                }.getOrNull()
            }
        }
        val isAvailable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            communicationDevices().any { it.isBluetoothHeadset() }
        } else {
            val proxyAvailable = runCatching {
                bluetoothHeadsetProxy?.connectedDevices?.isNotEmpty() == true
            }.getOrDefault(false)
            proxyAvailable || audioOutputDevices().any { it.isBluetoothHeadset() }
        }
        return if (isAvailable) AudioDevice.BluetoothHeadset(name ?: "Bluetooth") else null
    }

    private fun communicationDevices(): List<AudioDeviceInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return emptyList()
        return runCatching { audioManager.availableCommunicationDevices }.getOrDefault(emptyList())
    }

    private fun audioOutputDevices(): Array<AudioDeviceInfo> {
        return runCatching { audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS) }
            .getOrDefault(emptyArray())
    }

    private fun routeDevice(device: AudioDevice): Boolean {
        synchronized(lock) {
            if (state != State.ACTIVATED || selectedDevice != device) return true
            return runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    routeWithCommunicationDeviceLocked(device)
                } else {
                    routeWithLegacyAudioManagerLocked(device)
                }
            }.onFailure { log("Route $device failed: $it") }.getOrDefault(false)
        }
    }

    @SuppressLint("NewApi")
    private fun routeWithCommunicationDeviceLocked(device: AudioDevice): Boolean {
        val communicationDevice = communicationDevices().firstOrNull { it.matches(device) }
        if (communicationDevice != null && audioManager.setCommunicationDevice(communicationDevice)) {
            bluetoothScoActive = device is AudioDevice.BluetoothHeadset
            bluetoothScoConnecting = false
            bluetoothActivationError = false
            cancelBluetoothScoTimeoutLocked()
            return true
        }
        if (device is AudioDevice.BluetoothHeadset) return false

        audioManager.clearCommunicationDevice()
        audioManager.isSpeakerphoneOn = device is AudioDevice.Speakerphone
        bluetoothScoActive = false
        bluetoothScoConnecting = false
        cancelBluetoothScoTimeoutLocked()
        return true
    }

    @Suppress("DEPRECATION")
    private fun routeWithLegacyAudioManagerLocked(device: AudioDevice): Boolean {
        stopBluetoothScoLocked()
        if (device is AudioDevice.BluetoothHeadset) {
            audioManager.isSpeakerphoneOn = false
            audioManager.startBluetoothSco()
            bluetoothScoConnecting = true
            bluetoothScoActive = false
            scheduleBluetoothScoTimeoutLocked()
        } else {
            audioManager.isBluetoothScoOn = false
            audioManager.isSpeakerphoneOn = device is AudioDevice.Speakerphone
        }
        return true
    }

    private fun markBluetoothActivationError() {
        synchronized(lock) {
            bluetoothActivationError = true
            userSelectedDevice = null
        }
    }

    @Suppress("DEPRECATION")
    private fun onBluetoothScoConnected() {
        synchronized(lock) {
            bluetoothScoConnecting = false
            bluetoothScoActive = true
            bluetoothActivationError = false
            cancelBluetoothScoTimeoutLocked()
            if (state == State.ACTIVATED && selectedDevice is AudioDevice.BluetoothHeadset) {
                audioManager.isBluetoothScoOn = true
                audioManager.isSpeakerphoneOn = false
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun onBluetoothScoDisconnected() {
        var shouldRefresh = false
        synchronized(lock) {
            bluetoothScoActive = false
            if (bluetoothScoConnecting) return
            if (state == State.ACTIVATED && selectedDevice is AudioDevice.BluetoothHeadset) {
                shouldRefresh = bluetoothHeadsetDeviceLocked() == null
            }
        }
        if (shouldRefresh) refreshDevices()
    }

    private fun scheduleBluetoothScoTimeoutLocked() {
        cancelBluetoothScoTimeoutLocked()
        val timeout = Runnable {
            var shouldRefresh = false
            synchronized(lock) {
                if (state == State.ACTIVATED &&
                    selectedDevice is AudioDevice.BluetoothHeadset &&
                    bluetoothScoConnecting &&
                    !bluetoothScoActive
                ) {
                    bluetoothScoConnecting = false
                    bluetoothActivationError = true
                    userSelectedDevice = null
                    stopBluetoothScoLocked()
                    shouldRefresh = true
                }
            }
            if (shouldRefresh) refreshDevices()
        }
        bluetoothScoTimeoutRunnable = timeout
        handler.postDelayed(timeout, BLUETOOTH_SCO_TIMEOUT_MS)
    }

    @Suppress("DEPRECATION")
    private fun stopBluetoothScoLocked() {
        cancelBluetoothScoTimeoutLocked()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
            (bluetoothScoActive || bluetoothScoConnecting)
        ) {
            runCatching { audioManager.stopBluetoothSco() }
                .onFailure { log("Stop Bluetooth SCO failed: $it") }
        }
        bluetoothScoActive = false
        bluetoothScoConnecting = false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            runCatching { audioManager.isBluetoothScoOn = false }
                .onFailure { log("Disable Bluetooth SCO failed: $it") }
        }
    }

    private fun cancelBluetoothScoTimeoutLocked() {
        bluetoothScoTimeoutRunnable?.let(handler::removeCallbacks)
        bluetoothScoTimeoutRunnable = null
    }

    @SuppressLint("NewApi")
    private fun cacheAudioStateLocked() {
        savedAudioMode = audioManager.mode
        savedIsMicrophoneMuted = audioManager.isMicrophoneMute
        savedSpeakerphoneEnabled = audioManager.isSpeakerphoneOn
        savedCommunicationDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice
        } else {
            null
        }
        hasSavedAudioState = true
    }

    @SuppressLint("NewApi")
    private fun requestAudioFocusLocked() {
        audioManager.isMicrophoneMute = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            )
        }
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    @Suppress("DEPRECATION")
    @SuppressLint("NewApi")
    private fun restoreAudioStateLocked() {
        if (!hasSavedAudioState) return
        cancelBluetoothScoTimeoutLocked()
        stopBluetoothScoLocked()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val communicationDevice = savedCommunicationDevice
            if (communicationDevice != null && !audioManager.setCommunicationDevice(communicationDevice)) {
                audioManager.clearCommunicationDevice()
            } else if (communicationDevice == null) {
                audioManager.clearCommunicationDevice()
            }
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || savedCommunicationDevice == null) {
            audioManager.isSpeakerphoneOn = savedSpeakerphoneEnabled
        }
        audioManager.isMicrophoneMute = savedIsMicrophoneMuted
        audioManager.mode = savedAudioMode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let(audioManager::abandonAudioFocusRequest)
        } else {
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        audioFocusRequest = null
        hasSavedAudioState = false
    }

    private fun AudioDeviceInfo.matches(device: AudioDevice): Boolean {
        return when (device) {
            is AudioDevice.BluetoothHeadset -> isBluetoothHeadset()
            is AudioDevice.WiredHeadset -> isWiredHeadset()
            is AudioDevice.Earpiece -> type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            is AudioDevice.Speakerphone -> type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        }
    }

    private fun AudioDeviceInfo.isBluetoothHeadset(): Boolean {
        return type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET) ||
            type == AudioDeviceInfo.TYPE_HEARING_AID
    }

    private fun AudioDeviceInfo.isWiredHeadset(): Boolean {
        return type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            type == AudioDeviceInfo.TYPE_USB_HEADSET
    }

    private fun log(message: String) {
        if (loggingEnabled) Timber.d("$TAG_AUDIO $message")
    }

    private enum class State {
        STARTED,
        ACTIVATED,
        STOPPED,
    }

    companion object {
        private const val BLUETOOTH_SCO_TIMEOUT_MS = 5_000L
        private val defaultPreferredDeviceList = listOf(
            AudioDevice.BluetoothHeadset::class.java,
            AudioDevice.WiredHeadset::class.java,
            AudioDevice.Earpiece::class.java,
            AudioDevice.Speakerphone::class.java,
        )
    }
}

fun AudioSwitch.isBluetoothHeadsetOrWiredHeadset(): Boolean =
    selectedAudioDevice.isBluetoothHeadsetOrWiredHeadset()

fun AudioDevice?.isBluetoothHeadsetOrWiredHeadset(): Boolean =
    this is AudioDevice.BluetoothHeadset ||
        this is AudioDevice.WiredHeadset

fun AudioDevice?.isSpeakerOrEarpiece(): Boolean =
    this is AudioDevice.Speakerphone ||
        this is AudioDevice.Earpiece

fun AudioSwitch.selectSpeakerphone() =
    availableAudioDevices
        .find { it is AudioDevice.Speakerphone }
        ?.let(::selectDevice)

fun AudioSwitch.selectEarpiece() =
    availableAudioDevices
        .find { it is AudioDevice.Earpiece }
        ?.let(::selectDevice)

fun AudioSwitch.safeActivate() =
    try {
        activate()
    } catch (e: IllegalStateException) {
        Timber.w("$TAG_AUDIO AudioSwitch call activate() meet $e")
    }

fun AudioSwitch.safeStop() {
    try {
        stop()
    } catch (e: Exception) {
        Timber.w("$TAG_AUDIO AudioSwitch call stop() meet $e")
        if (e !is IllegalArgumentException) {
            reportException("$TAG_AUDIO AudioSwitch call stop()", e)
        }
    }
}
