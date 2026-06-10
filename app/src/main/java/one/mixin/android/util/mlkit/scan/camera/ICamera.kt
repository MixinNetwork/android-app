package one.mixin.android.util.mlkit.scan.camera

import androidx.annotation.FloatRange
import androidx.camera.core.Camera

interface ICamera {
    fun startCamera()

    fun stopCamera()

    fun getCamera(): Camera?

    fun release()

    // camera control
    fun zoomIn()

    fun zoomOut()

    fun zoomTo(ratio: Float)

    fun lineZoomIn()

    fun lineZoomOut()

    fun lineZoomTo(
        @FloatRange(from = 0.0, to = 1.0) linearZoom: Float,
    )

    fun enableTorch(torch: Boolean)

    fun isTorchEnabled(): Boolean

    fun hasFlashUnit(): Boolean
}
