package one.mixin.android.util.mlkit.scan.camera.config

import android.content.Context
import android.util.DisplayMetrics
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import timber.log.Timber

class ResolutionCameraConfig(context: Context) : CameraConfig() {
    private var targetSize: Size

    init {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val width: Int = displayMetrics.widthPixels
        val height: Int = displayMetrics.heightPixels
        Timber.d(String.format("displayMetrics:%d x %d", width, height))
        targetSize =
            if (width < height) {
                val size = width.coerceAtMost(1080)
                val ratio = width / height.toFloat()
                if (ratio > 0.7) {
                    Size(size, (size / 3.0f * 4.0f).toInt())
                } else {
                    Size(size, (size / 9.0f * 16.0f).toInt())
                }
            } else {
                val size = height.coerceAtMost(1080)
                val ratio = height / width.toFloat()
                if (ratio > 0.7) {
                    Size((size / 3.0f * 4.0f).toInt(), size)
                } else {
                    Size((size / 9.0f * 16.0).toInt(), size)
                }
            }
        Timber.d("targetSize:$targetSize")
    }

    override fun options(builder: Preview.Builder): Preview {
        return super.options(builder)
    }

    override fun options(builder: CameraSelector.Builder): CameraSelector {
        return super.options(builder)
    }

    override fun options(builder: ImageAnalysis.Builder): ImageAnalysis {
        builder.setTargetResolution(targetSize)
        return super.options(builder)
    }
}
