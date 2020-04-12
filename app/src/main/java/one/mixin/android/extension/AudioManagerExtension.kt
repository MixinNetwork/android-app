package one.mixin.android.extension

import android.media.AudioDeviceInfo
import android.media.AudioManager

fun AudioManager.isHeadsetOn(): Boolean {
    val devices = getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    devices.forEach { device ->
        if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            device.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
            return true
        }
    }
    return false
}
