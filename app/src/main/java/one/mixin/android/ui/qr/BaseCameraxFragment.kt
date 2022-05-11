package one.mixin.android.ui.qr

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Size
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.View
import android.view.ViewConfiguration
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.FocusMeteringResult
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.BuildConfig
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.bounce
import one.mixin.android.extension.decodeQR
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getStackTraceString
import one.mixin.android.extension.heavyClickVibrate
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.isDonateUrl
import one.mixin.android.extension.matchResourcePattern
import one.mixin.android.extension.notNullWithElse
import one.mixin.android.extension.openGallery
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.job.RefreshExternalSchemeJob.Companion.PREF_EXTERNAL_SCHEMES
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.ui.web.WebActivity
import one.mixin.android.util.reportException
import one.mixin.android.widget.gallery.ui.GalleryActivity
import timber.log.Timber
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

abstract class BaseCameraxFragment : VisionFragment() {
    companion object {
        const val CRASHLYTICS_CAMERAX = "camerax"

        private const val UNITY_ZOOM_SCALE = 1f
        private const val ZOOM_NOT_SUPPORTED = UNITY_ZOOM_SCALE
    }

    protected var forScanResult: Boolean = false

    protected var lensFacing = CameraSelector.LENS_FACING_BACK

    private var alreadyDetected = false

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector? = null

    private var imageAnalysis: ImageAnalysis? = null

    private var preview: Preview? = null
    protected lateinit var mainExecutor: Executor
    private lateinit var backgroundExecutor: ExecutorService
    protected var camera: Camera? = null

    private var displayId: Int = -1
    private lateinit var displayManager: DisplayManager
    lateinit var metrics: DisplayMetrics
    private var downEventTimestamp = 0L
    private var upEvent: MotionEvent? = null

    private val pinchToZoomGestureDetector: PinchToZoomGestureDetector by lazy {
        PinchToZoomGestureDetector(requireContext())
    }
    private var isPinchToZoomEnabled = true
    private var isZoomSupported = true

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@BaseCameraxFragment.displayId) {
                imageAnalysis?.targetRotation = view.display.rotation
                this@BaseCameraxFragment.onDisplayChanged(view.display.rotation)
            }
        } ?: Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainExecutor = ContextCompat.getMainExecutor(requireContext())
        backgroundExecutor = Executors.newSingleThreadExecutor()
    }

    abstract fun getContentView(): View
    private val _contentView get() = getContentView()

    private val close: View by lazy {
        _contentView.findViewById(R.id.close)
    }
    private val flash: View by lazy {
        _contentView.findViewById(R.id.flash)
    }
    private val galleryIv: View by lazy {
        _contentView.findViewById(R.id.gallery_iv)
    }
    private val viewFinder: PreviewView by lazy {
        _contentView.findViewById(R.id.view_finder)
    }
    private val focusView: FocusView by lazy {
        _contentView.findViewById(R.id.focus_view)
    }

    @SuppressLint("RestrictedApi", "ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        close.setOnClickListener { activity?.onBackPressed() }
        flash.setOnClickListener {
            onFlashClick()
            flash.bounce()
        }
        galleryIv.setOnClickListener {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            openGallery()
                        } else {
                            context?.openPermissionSetting()
                        }
                    },
                    {
                    }
                )
        }
        checkFlash()

        displayManager = viewFinder.context
            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        setZoomRatio(UNITY_ZOOM_SCALE)
        viewFinder.setOnTouchListener { v, event ->
            if (isPinchToZoomEnabled) {
                pinchToZoomGestureDetector.onTouchEvent(event)
            }
            if (event.pointerCount == 2 && isPinchToZoomEnabled && isZoomSupported) {
                return@setOnTouchListener true
            }

            when (event.action) {
                ACTION_DOWN -> {
                    downEventTimestamp = System.currentTimeMillis()
                }
                ACTION_UP -> {
                    if (delta() < ViewConfiguration.getLongPressTimeout()) {
                        upEvent = event
                        return@setOnTouchListener focusAndMeter(v as PreviewView)
                    }
                }
                else -> return@setOnTouchListener false
            }
            return@setOnTouchListener true
        }
        viewFinder.post {
            displayId = viewFinder.display.displayId
            bindCameraUseCase()
        }
    }

    @SuppressLint("RestrictedApi")
    protected fun bindCameraUseCase() {
        metrics = DisplayMetrics().also { viewFinder.display.getRealMetrics(it) }
        val rotation = getRotation()

        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()

                val useCases = arrayListOf<UseCase>()
                preview = Preview.Builder()
                    .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
                    .setTargetRotation(rotation)
                    .build()

                useCases.add(preview!!)
                useCases.add(getImageAnalysis())
                appendOtherUseCases(useCases, rotation)

                cameraProvider?.unbindAll()

                try {
                    camera = cameraProvider?.bindToLifecycle(
                        this as LifecycleOwner,
                        cameraSelector!!,
                        *useCases.toTypedArray()
                    )
                    preview?.setSurfaceProvider(viewFinder.surfaceProvider)
                } catch (e: Exception) {
                    reportException("$CRASHLYTICS_CAMERAX-camera bindToLifecycle failure", e)
                    Timber.w(e)
                }
            },
            mainExecutor
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        backgroundExecutor.shutdown()
        displayManager.unregisterDisplayListener(displayListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                val path = it.getFilePath(MixinApplication.get())
                if (path == null) {
                    toast(R.string.File_error)
                } else {
                    if (data.hasExtra(GalleryActivity.IS_VIDEO)) {
                        openEdit(path, true, fromGallery = true)
                    } else {
                        openEdit(path, false, fromGallery = true)
                    }
                }
            }
        }
    }

    protected fun bindUseCases(vararg useCases: UseCase) {
        try {
            camera = cameraProvider?.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector!!,
                *useCases,
            )
        } catch (e: Exception) {
            reportException("$CRASHLYTICS_CAMERAX-bindUseCases", e)
            Timber.w(e)
        }
    }

    protected fun unbindUseCases(vararg useCases: UseCase) {
        try {
            cameraProvider?.unbind(*useCases)
        } catch (e: Exception) {
            reportException("$CRASHLYTICS_CAMERAX-unbindUseCases", e)
            Timber.w(e)
        }
    }

    protected fun stopImageAnalysis() {
        if (imageAnalysis != null) {
            try {
                imageAnalysis?.clearAnalyzer()
                cameraProvider?.unbind(imageAnalysis!!)
                imageAnalysis = null
            } catch (e: Exception) {
                reportException("$CRASHLYTICS_CAMERAX-stopImageAnalysis", e)
                Timber.w(e)
            }
        }
    }

    protected fun startImageAnalysis() {
        val localImageAnalysis = getImageAnalysis()
        try {
            camera = cameraProvider?.bindToLifecycle(
                this as LifecycleOwner,
                cameraSelector!!,
                localImageAnalysis,
            )
        } catch (e: Exception) {
            reportException("$CRASHLYTICS_CAMERAX-startImageAnalysis", e)
            Timber.w(e)
        }
    }

    private fun getImageAnalysis(): ImageAnalysis {
        if (imageAnalysis != null) return imageAnalysis!!

        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
            .setTargetRotation(getRotation())
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor, imageAnalyzer)
            }
        return imageAnalysis!!
    }

    fun startImageAnalysisIfNeeded() {
        if (imageAnalysis == null) {
            startImageAnalysis()
        }
    }

    private fun getRotation(): Int = viewFinder.display?.rotation ?: Surface.ROTATION_0

    private fun isLensBack() = CameraSelector.LENS_FACING_BACK == lensFacing

    protected fun openEdit(path: String, isVideo: Boolean, fromGallery: Boolean = false) {
        activity?.supportFragmentManager?.inTransaction {
            add(R.id.container, EditFragment.newInstance(path, isVideo, fromGallery, fromScan()), EditFragment.TAG)
                .addToBackStack(null)
        }
    }

    protected fun checkFlash() {
        if (isLensBack()) {
            flash.visibility = View.VISIBLE
        } else {
            flash.visibility = View.GONE
        }
    }

    private fun getZoomRatio(): Float =
        camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: UNITY_ZOOM_SCALE

    @SuppressLint("RestrictedApi")
    private fun setZoomRatio(zoomRatio: Float) {
        camera?.let {
            val future = it.cameraControl.setZoomRatio(zoomRatio)
            Futures.addCallback(
                future,
                object : FutureCallback<Void> {
                    override fun onSuccess(result: Void?) {
                    }

                    override fun onFailure(t: Throwable?) {
                        t?.let { throwable ->
                            if (throwable is CameraControl.OperationCanceledException) {
                                Timber.d("$CRASHLYTICS_CAMERAX-setZoomRatio onFailure, ${throwable.getStackTraceString()}")
                                return
                            }
                            if (BuildConfig.DEBUG) {
                                Timber.w("$CRASHLYTICS_CAMERAX-setZoomRatio onFailure, ${throwable.getStackTraceString()}")
                            } else {
                                reportException("$CRASHLYTICS_CAMERAX-setZoomRatio onFailure", throwable)
                            }
                        }
                    }
                },
                mainExecutor
            )
        }
    }

    @SuppressLint("RestrictedApi")
    private fun focusAndMeter(v: PreviewView): Boolean {
        var x = 0f
        var y = 0f
        upEvent.notNullWithElse(
            {
                x = it.x
                y = it.y
            },
            {
                x = v.x + v.width / 2f
                y = v.y + v.height / 2f
            }
        )
        upEvent = null
        focusView.focusAndMeter(x, y)

        camera?.let { c ->
            val pointFactory = v.meteringPointFactory
            val afPointWidth = 1.0f / 6.0f
            val aePointWidth = afPointWidth * 1.5f
            val afPoint = pointFactory.createPoint(x, y, afPointWidth)
            val aePoint = pointFactory.createPoint(x, y, aePointWidth)

            val future = c.cameraControl.startFocusAndMetering(
                FocusMeteringAction.Builder(
                    afPoint,
                    FocusMeteringAction.FLAG_AF
                ).addPoint(
                    aePoint,
                    FocusMeteringAction.FLAG_AE
                ).build()
            )
            Futures.addCallback(
                future,
                object : FutureCallback<FocusMeteringResult> {
                    override fun onSuccess(result: FocusMeteringResult?) {
                    }

                    override fun onFailure(t: Throwable?) {
                        t?.let { throwable ->
                            if (throwable is CameraControl.OperationCanceledException) {
                                Timber.d("$CRASHLYTICS_CAMERAX-focusAndMeter onFailure, ${throwable.getStackTraceString()}")
                                return
                            }
                            if (BuildConfig.DEBUG) {
                                Timber.w("$CRASHLYTICS_CAMERAX-focusAndMeter onFailure, ${throwable.getStackTraceString()}")
                            } else {
                                reportException("$CRASHLYTICS_CAMERAX-focusAndMeter onFailure", throwable)
                            }
                        }
                    }
                },
                mainExecutor
            )
        }
        return true
    }

    protected fun getMaxZoomRatio(): Float =
        camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: ZOOM_NOT_SUPPORTED

    protected fun getMinZoomRation(): Float =
        camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: UNITY_ZOOM_SCALE

    private fun rangeLimit(value: Float, max: Float, min: Float) =
        min(max(value, min), max)

    private fun delta() = System.currentTimeMillis() - downEventTimestamp

    abstract fun onFlashClick()
    abstract fun appendOtherUseCases(useCases: ArrayList<UseCase>, rotation: Int)
    abstract fun onDisplayChanged(rotation: Int)
    abstract fun fromScan(): Boolean

    private fun handleAnalysis(analysisResult: String) {
        if (viewDestroyed()) return

        requireContext().heavyClickVibrate()
        requireContext().defaultSharedPreferences.putBoolean(CaptureActivity.SHOW_QR_CODE, false)
        if (forScanResult) {
            val scanResult = if (analysisResult.isDonateUrl()) {
                val index = analysisResult.indexOf("?")
                if (index != -1) {
                    analysisResult.take(index)
                } else analysisResult
            } else analysisResult
            val result = Intent().apply {
                putExtra(CaptureActivity.ARGS_FOR_SCAN_RESULT, scanResult)
            }
            activity?.setResult(Activity.RESULT_OK, result)
            activity?.finish()
            return
        }
        if (analysisResult.startsWith(Constants.Scheme.DEVICE)) {
            ConfirmBottomFragment.show(requireContext(), parentFragmentManager, analysisResult) {
                activity?.finish()
            }
        } else {
            if (fromScan()) {
                val externalSchemes = requireContext().defaultSharedPreferences.getStringSet(PREF_EXTERNAL_SCHEMES, emptySet())
                if (!externalSchemes.isNullOrEmpty() && analysisResult.matchResourcePattern(externalSchemes)) {
                    WebActivity.show(requireContext(), analysisResult, null)
                    activity?.finish()
                    return
                }
                handleResult(analysisResult)
            } else {
                pseudoNotificationView?.addContent(analysisResult)
            }
        }
    }

    private val imageAnalyzer = object : ImageAnalysis.Analyzer {
        private val detecting = AtomicBoolean(false)

        override fun analyze(image: ImageProxy) {
            if (!alreadyDetected && !image.planes.isNullOrEmpty() &&
                detecting.compareAndSet(false, true)
            ) {
                try {
                    decodeWithFirebaseVision(image)
                } catch (e: Exception) {
                    decodeWithZxing(image)
                    reportException("$CRASHLYTICS_CAMERAX-decodeWithFirebaseVision failure", e)
                }
            } else {
                image.close()
            }
        }

        @SuppressLint("UnsafeExperimentalUsageError", "UnsafeOptInUsageError")
        private fun decodeWithFirebaseVision(image: ImageProxy) {
            val processImage = image.image
            if (processImage == null) {
                image.close()
                return
            }
            val inputImage = InputImage.fromMediaImage(processImage, image.imageInfo.rotationDegrees)
            val latch = CountDownLatch(1)
            scanner.process(inputImage)
                .addOnSuccessListener { result ->
                    result.firstOrNull()?.rawValue?.let {
                        alreadyDetected = true
                        handleAnalysis(it)
                    }
                }
                .addOnCompleteListener {
                    if (!alreadyDetected) {
                        val bitmap = getBitmapFromImage(image)
                        if (bitmap == null) {
                            detecting.set(false)
                        } else {
                            decodeBitmapWithZxing(bitmap)
                        }
                    } else {
                        detecting.set(false)
                    }
                    image.close()
                    latch.countDown()
                }
            latch.await()
        }

        private fun decodeWithZxing(imageProxy: ImageProxy) {
            val bitmap = getBitmapFromImage(imageProxy)
            if (bitmap == null) {
                detecting.set(false)
                return
            }
            val result = bitmap.decodeQR()
            if (result != null) {
                alreadyDetected = true
                lifecycleScope.launch(Dispatchers.Main) {
                    if (viewDestroyed()) return@launch
                    handleAnalysis(result)
                }
            }
            imageProxy.close()
            detecting.set(false)
        }

        private fun decodeBitmapWithZxing(bitmap: Bitmap) {
            val result = bitmap.decodeQR()
            if (result != null) {
                alreadyDetected = true
                lifecycleScope.launch(Dispatchers.Main) {
                    if (viewDestroyed()) return@launch
                    handleAnalysis(result)
                }
            }
            detecting.set(false)
        }

        private fun getBitmapFromImage(image: ImageProxy): Bitmap? {
            return try {
                val byteArray = ImageUtil.imageToJpegByteArray(image)
                BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            } catch (e: Exception) {
                reportException("$CRASHLYTICS_CAMERAX-getBitmapFromImage failure", e)
                null
            }
        }
    }

    class S : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        lateinit var listener: ScaleGestureDetector.OnScaleGestureListener

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            return listener.onScale(detector)
        }
    }

    inner class PinchToZoomGestureDetector(
        context: Context,
        s: S = S()
    ) : ScaleGestureDetector(context, s), ScaleGestureDetector.OnScaleGestureListener {
        init {
            s.listener = this
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            var scale = detector?.scaleFactor ?: return true

            scale = if (scale > 1f) {
                1.0f + (scale - 1.0f) * 2
            } else {
                1.0f - (1.0f - scale) * 2
            }

            var newRatio = getZoomRatio() * scale
            newRatio = rangeLimit(newRatio, getMaxZoomRatio(), getMinZoomRation())
            setZoomRatio(newRatio)
            return true
        }

        override fun onScaleBegin(detector: ScaleGestureDetector?) = true

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
        }
    }
}

val donateSupported = arrayOf(
    "bitcoin:", "bitcoincash:", "bitcoinsv:", "ethereum:",
    "litecoin:", "dash:", "ripple:", "zcash:", "horizen:", "monero:", "binancecoin:",
    "stellar:", "dogecoin:", "mobilecoin:"
)
