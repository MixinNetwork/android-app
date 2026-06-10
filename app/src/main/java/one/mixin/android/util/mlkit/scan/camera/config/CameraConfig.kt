package one.mixin.android.util.mlkit.scan.camera.config

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview

open class CameraConfig {
    open fun options(builder: Preview.Builder): Preview {
        return builder.build()
    }

    open fun options(builder: CameraSelector.Builder): CameraSelector {
        return builder.build()
    }

    open fun options(builder: ImageAnalysis.Builder): ImageAnalysis {
        return builder.build()
    }
}
