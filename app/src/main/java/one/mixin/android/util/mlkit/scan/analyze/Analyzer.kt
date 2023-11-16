package one.mixin.android.util.mlkit.scan.analyze

import androidx.camera.core.ImageProxy

interface Analyzer<T> {
    fun analyze(
        imageProxy: ImageProxy,
        listener: OnAnalyzeListener<AnalyzeResult<T>>,
    )

    interface OnAnalyzeListener<T> {
        fun onSuccess(result: T)

        fun onFailure()
    }
}
