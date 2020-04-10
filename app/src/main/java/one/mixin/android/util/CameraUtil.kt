@file:Suppress("DEPRECATION")

package one.mixin.android.util

import android.hardware.Camera

fun isCameraCanUse(): Boolean {
    var canUse = true
    var camera: Camera? = null
    try {
        camera = Camera.open()

        // Meizu MX5 Camera.open() not return null
        val parameters = camera.parameters
        camera.parameters = parameters
    } catch (e: Exception) {
        canUse = false
    }
    camera?.release()
    return canUse
}
