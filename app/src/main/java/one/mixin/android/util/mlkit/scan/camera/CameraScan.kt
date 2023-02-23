package one.mixin.android.util.mlkit.scan.camera

import android.content.Intent
import android.view.View
import androidx.camera.core.CameraSelector
import one.mixin.android.util.mlkit.scan.analyze.AnalyzeResult
import one.mixin.android.util.mlkit.scan.analyze.Analyzer
import one.mixin.android.util.mlkit.scan.camera.config.CameraConfig

abstract class CameraScan<T> : ICamera {
    protected var isNeedTouchZoom = true
        private set

    fun setNeedTouchZoom(needTouchZoom: Boolean): CameraScan<T> {
        isNeedTouchZoom = needTouchZoom
        return this
    }

    abstract fun setCameraConfig(cameraConfig: CameraConfig?): CameraScan<T>

    abstract fun setAnalyzeImage(analyze: Boolean): CameraScan<T>

    abstract fun setAnalyzer(analyzer: Analyzer<T>?): CameraScan<T>

    abstract fun setVibrate(vibrate: Boolean): CameraScan<T>

    abstract fun setPlayBeep(playBeep: Boolean): CameraScan<T>

    abstract fun setOnScanResultCallback(callback: OnScanResultCallback<T>): CameraScan<T>

    abstract fun bindFlashlightView(v: View?): CameraScan<T>

    abstract fun setDarkLightLux(lightLux: Float): CameraScan<T>

    abstract fun setBrightLightLux(lightLux: Float): CameraScan<T>
    interface OnScanResultCallback<T> {

        fun onScanResultCallback(result: AnalyzeResult<T>)

        fun onScanResultFailure() {}
    }

    companion object {
        var SCAN_RESULT = "SCAN_RESULT"

        var LENS_FACING_FRONT = CameraSelector.LENS_FACING_FRONT

        var LENS_FACING_BACK = CameraSelector.LENS_FACING_BACK

        fun parseScanResult(data: Intent?): String? {
            return data?.getStringExtra(SCAN_RESULT)
        }
    }
}
