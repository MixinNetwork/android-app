package one.mixin.android.extension

import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch
import one.mixin.android.util.reportException
import one.mixin.android.webrtc.TAG_AUDIO
import timber.log.Timber

fun AudioSwitch.isBluetoothHeadsetOrWiredHeadset(): Boolean =
    selectedAudioDevice.isBluetoothHeadsetOrWiredHeadset()

fun AudioDevice?.isBluetoothHeadsetOrWiredHeadset(): Boolean =
    this is AudioDevice.BluetoothHeadset ||
        this is AudioDevice.WiredHeadset

fun AudioDevice?.isSpeakerOrEarpiece(): Boolean =
    this is AudioDevice.Speakerphone ||
        this is AudioDevice.Earpiece

fun AudioSwitch.selectSpeakerphone() =
    this.availableAudioDevices
        .find { it is AudioDevice.Speakerphone }
        ?.let { this.selectDevice(it) }

fun AudioSwitch.selectEarpieceAndActivate() =
    this.availableAudioDevices
        .find { it is AudioDevice.Earpiece }
        ?.let {
            this.selectDevice(it)
            safeActivate()
        }

fun AudioSwitch.safeActivate() = try {
    activate()
} catch (e: IllegalStateException) {
    Timber.w("$TAG_AUDIO AudioSwitch call active() meet $e")
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
