package one.mixin.android.extension

import com.twilio.audioswitch.AudioDevice
import com.twilio.audioswitch.AudioSwitch

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

fun AudioSwitch.selectEarpiece() =
    this.availableAudioDevices
        .find { it is AudioDevice.Earpiece }
        ?.let { this.selectDevice(it) }

fun AudioSwitch.safeActivate() = try {
    activate()
} catch (e: IllegalStateException) {
}
