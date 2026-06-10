package one.mixin.android.util.mlkit.scan

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import android.view.View
import androidx.annotation.FloatRange
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.common.util.concurrent.ListenableFuture
import one.mixin.android.util.mlkit.scan.analyze.AnalyzeResult
import one.mixin.android.util.mlkit.scan.analyze.Analyzer
import one.mixin.android.util.mlkit.scan.analyze.Analyzer.OnAnalyzeListener
import one.mixin.android.util.mlkit.scan.camera.CameraScan
import one.mixin.android.util.mlkit.scan.camera.config.CameraConfig
import one.mixin.android.util.mlkit.scan.manager.AmbientLightManager
import one.mixin.android.util.mlkit.scan.manager.AmbientLightManager.OnLightSensorEventListener
import one.mixin.android.util.mlkit.scan.manager.BeepManager
import one.mixin.android.util.reportException
import timber.log.Timber
import java.util.concurrent.Executors

class BaseCameraScan<T> : CameraScan<T> {
    private var fragmentActivity: FragmentActivity
    private var context: Context
    private var lifecycleOwner: LifecycleOwner
    private var previewView: PreviewView

    private var mCameraConfig: CameraConfig? = null

    private var mCameraProviderFuture: ListenableFuture<ProcessCameraProvider>? = null
    private var mCamera: Camera? = null
    private var mAnalyzer: Analyzer<T>? = null

    @Volatile
    private var isAnalyze = true

    @Volatile
    private var isAnalyzeResult = false
    private var flashlightView: View? = null
    private var mResultLiveData: MutableLiveData<AnalyzeResult<T>?>? = null
    private var mOnScanResultCallback: OnScanResultCallback<T>? = null
    private var mOnAnalyzeListener: OnAnalyzeListener<AnalyzeResult<T>>? = null
    private var mBeepManager: BeepManager? = null
    private var mAmbientLightManager: AmbientLightManager? = null
    private var mLastHoveTapTime: Long = 0
    private var isClickTap = false
    private var mDownX = 0f
    private var mDownY = 0f

    constructor(activity: FragmentActivity, previewView: PreviewView) {
        fragmentActivity = activity
        lifecycleOwner = activity
        context = activity
        this.previewView = previewView
        initData()
    }

    constructor(fragment: Fragment, previewView: PreviewView) {
        fragmentActivity = fragment.requireActivity()
        lifecycleOwner = fragment
        context = fragment.requireContext()
        this.previewView = previewView
        initData()
    }

    private val mOnScaleGestureListener: OnScaleGestureListener =
        object : SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val ratio = mCamera?.cameraInfo?.zoomState?.value?.zoomRatio ?: return false
                val scale = detector.scaleFactor
                zoomTo(ratio * scale)
                return true
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    private fun initData() {
        mResultLiveData = MutableLiveData()
        mResultLiveData?.observe(lifecycleOwner) { result: AnalyzeResult<T>? ->
            isAnalyzeResult = false
            if (result != null) {
                handleAnalyzeResult(result)
            } else if (mOnScanResultCallback != null) {
                mOnScanResultCallback?.onScanResultFailure()
            }
        }
        mOnAnalyzeListener =
            object : OnAnalyzeListener<AnalyzeResult<T>> {
                override fun onSuccess(result: AnalyzeResult<T>) {
                    mResultLiveData?.postValue(result)
                }

                override fun onFailure() {
                    mResultLiveData?.postValue(null)
                }
            }
        val scaleGestureDetector = ScaleGestureDetector(context, mOnScaleGestureListener)
        previewView.setOnTouchListener { v: View?, event: MotionEvent ->
            handlePreviewViewClickTap(event)
            if (isNeedTouchZoom) {
                return@setOnTouchListener scaleGestureDetector.onTouchEvent(event)
            }
            false
        }
        mBeepManager = BeepManager(context)
        mAmbientLightManager = AmbientLightManager(context)
        mAmbientLightManager?.register()
        mAmbientLightManager?.setOnLightSensorEventListener(
            object : OnLightSensorEventListener {
                override fun onSensorChanged(
                    dark: Boolean,
                    lightLux: Float,
                ) {
                    if (flashlightView != null) {
                        if (dark) {
                            if (flashlightView?.visibility != View.VISIBLE) {
                                flashlightView?.visibility = View.VISIBLE
                                flashlightView?.isSelected = isTorchEnabled()
                            }
                        } else if (flashlightView?.visibility == View.VISIBLE && !isTorchEnabled()) {
                            flashlightView?.visibility = View.INVISIBLE
                            flashlightView?.isSelected = false
                        }
                    }
                }
            },
        )
    }

    private fun handlePreviewViewClickTap(event: MotionEvent) {
        if (event.pointerCount == 1) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isClickTap = true
                    mDownX = event.x
                    mDownY = event.y
                    mLastHoveTapTime = System.currentTimeMillis()
                }

                MotionEvent.ACTION_MOVE ->
                    isClickTap =
                        distance(mDownX, mDownY, event.x, event.y) < HOVER_TAP_SLOP

                MotionEvent.ACTION_UP ->
                    if (isClickTap && mLastHoveTapTime + HOVER_TAP_TIMEOUT > System.currentTimeMillis()) {
                        startFocusAndMetering(event.x, event.y)
                    }
            }
        }
    }

    private fun distance(
        aX: Float,
        aY: Float,
        bX: Float,
        bY: Float,
    ): Float {
        val xDiff = aX - bX
        val yDiff = aY - bY
        return Math.sqrt((xDiff * xDiff + yDiff * yDiff).toDouble()).toFloat()
    }

    private fun startFocusAndMetering(
        x: Float,
        y: Float,
    ) {
        if (mCamera != null) {
            val point = previewView.meteringPointFactory.createPoint(x, y)
            val focusMeteringAction = FocusMeteringAction.Builder(point).build()
            if (mCamera!!.cameraInfo.isFocusMeteringSupported(focusMeteringAction)) {
                mCamera!!.cameraControl.startFocusAndMetering(focusMeteringAction)
                Timber.d("startFocusAndMetering:$x,$y")
            }
        }
    }

    private fun initConfig() {
        if (mCameraConfig == null) {
            mCameraConfig = CameraConfig()
        }
    }

    override fun setCameraConfig(cameraConfig: CameraConfig?): CameraScan<T> {
        if (cameraConfig != null) {
            mCameraConfig = cameraConfig
        }
        return this
    }

    override fun startCamera() {
        initConfig()
        val cameraConfig = mCameraConfig ?: return
        mCameraProviderFuture = ProcessCameraProvider.getInstance(context)
        mCameraProviderFuture!!.addListener({
            try {
                val preview = cameraConfig.options(Preview.Builder())

                val cameraSelector = cameraConfig.options(CameraSelector.Builder())
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val imageAnalysis =
                    cameraConfig.options(
                        ImageAnalysis.Builder()
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST),
                    )
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { image: ImageProxy ->
                    if (isAnalyze && !isAnalyzeResult && mAnalyzer != null) {
                        isAnalyzeResult = true
                        mAnalyzer!!.analyze(image, mOnAnalyzeListener!!)
                    }
                    image.close()
                }
                if (mCamera != null) {
                    mCameraProviderFuture!!.get().unbindAll()
                }
                // bind to the lifecycle
                mCamera =
                    mCameraProviderFuture!!.get()
                        .bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                reportException(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    @Synchronized
    private fun handleAnalyzeResult(result: AnalyzeResult<T>) {
        if (isAnalyzeResult || !isAnalyze) {
            return
        }
        mBeepManager?.playBeepSoundAndVibrate()
        mOnScanResultCallback?.onScanResultCallback(result)
    }

    override fun stopCamera() {
        if (mCameraProviderFuture != null) {
            try {
                mCameraProviderFuture!!.get().unbindAll()
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }

    override fun setAnalyzeImage(analyze: Boolean): CameraScan<T> {
        isAnalyze = analyze
        return this
    }

    override fun setAnalyzer(analyzer: Analyzer<T>?): CameraScan<T> {
        mAnalyzer = analyzer
        return this
    }

    override fun zoomIn() {
        if (mCamera != null) {
            val ratio = mCamera!!.cameraInfo.zoomState.value!!.zoomRatio + 0.1f
            val maxRatio = mCamera!!.cameraInfo.zoomState.value!!.maxZoomRatio
            if (ratio <= maxRatio) {
                mCamera!!.cameraControl.setZoomRatio(ratio)
            }
        }
    }

    override fun zoomOut() {
        if (mCamera != null) {
            val ratio = mCamera!!.cameraInfo.zoomState.value!!.zoomRatio - 0.1f
            val minRatio = mCamera!!.cameraInfo.zoomState.value!!.minZoomRatio
            if (ratio >= minRatio) {
                mCamera!!.cameraControl.setZoomRatio(ratio)
            }
        }
    }

    override fun zoomTo(ratio: Float) {
        if (mCamera != null) {
            val zoomState = mCamera!!.cameraInfo.zoomState.value
            val maxRatio = zoomState!!.maxZoomRatio
            val minRatio = zoomState.minZoomRatio
            val zoom = ratio.coerceAtMost(maxRatio).coerceAtLeast(minRatio)
            mCamera!!.cameraControl.setZoomRatio(zoom)
        }
    }

    override fun lineZoomIn() {
        if (mCamera != null) {
            val zoom = mCamera!!.cameraInfo.zoomState.value!!.linearZoom + 0.1f
            if (zoom <= 1f) {
                mCamera!!.cameraControl.setLinearZoom(zoom)
            }
        }
    }

    override fun lineZoomOut() {
        if (mCamera != null) {
            val zoom = mCamera!!.cameraInfo.zoomState.value!!.linearZoom - 0.1f
            if (zoom >= 0f) {
                mCamera!!.cameraControl.setLinearZoom(zoom)
            }
        }
    }

    override fun lineZoomTo(
        @FloatRange(from = 0.0, to = 1.0) linearZoom: Float,
    ) {
        if (mCamera != null) {
            mCamera!!.cameraControl.setLinearZoom(linearZoom)
        }
    }

    override fun enableTorch(torch: Boolean) {
        if (mCamera != null && hasFlashUnit()) {
            mCamera!!.cameraControl.enableTorch(torch)
        }
    }

    override fun isTorchEnabled(): Boolean {
        return if (mCamera != null) {
            mCamera!!.cameraInfo.torchState.value == TorchState.ON
        } else {
            false
        }
    }

    override fun hasFlashUnit(): Boolean {
        return if (mCamera != null) {
            mCamera!!.cameraInfo.hasFlashUnit()
        } else {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        }
    }

    override fun setVibrate(vibrate: Boolean): CameraScan<T> {
        if (mBeepManager != null) {
            mBeepManager!!.setVibrate(vibrate)
        }
        return this
    }

    override fun setPlayBeep(playBeep: Boolean): CameraScan<T> {
        if (mBeepManager != null) {
            mBeepManager!!.setPlayBeep(playBeep)
        }
        return this
    }

    override fun setOnScanResultCallback(callback: OnScanResultCallback<T>): CameraScan<T> {
        mOnScanResultCallback = callback
        return this
    }

    override fun getCamera(): Camera? {
        return mCamera
    }

    override fun release() {
        isAnalyze = false
        flashlightView = null
        if (mAmbientLightManager != null) {
            mAmbientLightManager!!.unregister()
        }
        if (mBeepManager != null) {
            mBeepManager!!.close()
        }
        stopCamera()
    }

    override fun bindFlashlightView(v: View?): CameraScan<T> {
        flashlightView = v
        if (mAmbientLightManager != null) {
            mAmbientLightManager!!.isLightSensorEnabled = v != null
        }
        return this
    }

    override fun setDarkLightLux(lightLux: Float): CameraScan<T> {
        if (mAmbientLightManager != null) {
            mAmbientLightManager!!.setDarkLightLux(lightLux)
        }
        return this
    }

    override fun setBrightLightLux(lightLux: Float): CameraScan<T> {
        if (mAmbientLightManager != null) {
            mAmbientLightManager!!.setBrightLightLux(lightLux)
        }
        return this
    }

    companion object {
        private const val HOVER_TAP_TIMEOUT = 150

        private const val HOVER_TAP_SLOP = 20
    }
}
