package one.mixin.android.util.mlkit.scan.camera.config

import android.content.Context
import android.util.DisplayMetrics
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.AspectRatioStrategy.FALLBACK_RULE_AUTO
import androidx.camera.core.resolutionselector.ResolutionSelector
import timber.log.Timber
import kotlin.math.abs

class AspectRatioCameraConfig(context: Context) : CameraConfig() {
    private val aspectRatio: Int

    init {
        val displayMetrics: DisplayMetrics = context.resources.displayMetrics
        val width: Int = displayMetrics.widthPixels
        val height: Int = displayMetrics.heightPixels
        aspectRatio = aspectRatio(width.toFloat(), height.toFloat())
        Timber.d("aspectRatio:$aspectRatio")
    }

    private fun aspectRatio(
        width: Float,
        height: Float,
    ): Int {
        val ratio = width.coerceAtLeast(height) / width.coerceAtMost(height)
        return if (abs(ratio - 4.0f / 3.0f) < abs(ratio - 16.0f / 9.0f)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    override fun options(builder: ImageAnalysis.Builder): ImageAnalysis {
        builder.setResolutionSelector(ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy(aspectRatio, FALLBACK_RULE_AUTO)).build())
        return super.options(builder)
    }
}
