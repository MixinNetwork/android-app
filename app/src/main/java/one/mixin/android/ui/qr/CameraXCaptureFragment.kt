package one.mixin.android.ui.qr

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.crashlytics.android.Crashlytics
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.android.synthetic.main.fragment_capture_camerax.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.decodeQR
import one.mixin.android.extension.getImageCachePath
import one.mixin.android.extension.isGooglePlayServicesAvailable
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_ACCOUNT_NAME
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_ADDRESS
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_MEMO

class CameraXCaptureFragment : BaseCaptureFragment() {
    companion object {
        const val TAG = "CameraXCaptureFragment"
        const val CRASHLYTICS_CAMERAX = "camerax"

        fun newInstance(
            forAddress: Boolean = false,
            forAccountName: Boolean = false,
            forMemo: Boolean = false
        ) = CameraXCaptureFragment().withArgs {
            putBoolean(ARGS_FOR_ADDRESS, forAddress)
            putBoolean(ARGS_FOR_ACCOUNT_NAME, forAccountName)
            putBoolean(ARGS_FOR_MEMO, forMemo)
        }
    }

    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var mainExecutor: Executor
    private lateinit var backgroundExecutor: Executor
    private var camera: Camera? = null

    private var displayId: Int = -1
    private lateinit var displayManager: DisplayManager

    private var imageCaptureFile: File? = null

    private var alreadyDetected = false

    private val forScan by lazy {
        arguments?.getBoolean(ARGS_FOR_ADDRESS) == true ||
            arguments?.getBoolean(ARGS_FOR_ACCOUNT_NAME) == true ||
            arguments?.getBoolean(ARGS_FOR_MEMO) == true
    }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@CameraXCaptureFragment.displayId) {
                imageCapture?.targetRotation = view.display.rotation
                imageAnalysis?.targetRotation = view.display.rotation
            }
        } ?: Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainExecutor = ContextCompat.getMainExecutor(requireContext())
        backgroundExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_capture_camerax, container, false)

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        displayManager = view_finder.context
            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        view_finder.post {
            displayId = view_finder.display.displayId
            bindCameraUseCase()
        }
        bottom_ll.isVisible = !forScan
    }

    override fun onFlashClick() {
        val torchState = camera?.cameraInfo?.torchState?.value ?: TorchState.OFF
        if (torchState == TorchState.ON) {
            flash.setImageResource(R.drawable.ic_flash_off)
            camera?.cameraControl?.enableTorch(false)
        } else {
            flash.setImageResource(R.drawable.ic_flash_on)
            camera?.cameraControl?.enableTorch(true)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onSwitchClick() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            flash.setImageResource(R.drawable.ic_flash_off)
            CameraSelector.LENS_FACING_FRONT
        }
        try {
            bindCameraUseCase()
        } catch (e: Exception) {
            Crashlytics.log(Log.ERROR, CRASHLYTICS_CAMERAX, "Switch lens and rebind use cases failure, $e")
        }
    }

    override fun isLensBack() = CameraSelector.LENS_FACING_BACK == lensFacing

    @SuppressLint("RestrictedApi")
    override fun onTakePicture() {
        imageCaptureFile = requireContext().getImageCachePath().createImageTemp()
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
        }
        val outputFileOptions =
            ImageCapture.OutputFileOptions.Builder(imageCaptureFile!!)
                .setMetadata(metadata)
                .build()
        imageCapture?.takePicture(outputFileOptions, mainExecutor, imageSavedListener)
    }

    @SuppressLint("RestrictedApi")
    private fun bindCameraUseCase() {
        val metrics = DisplayMetrics().also { view_finder.display.getRealMetrics(it) }
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)
        val rotation = view_finder.display.rotation

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()
                .setTargetAspectRatioCustom(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()
            preview?.setSurfaceProvider(view_finder.previewSurfaceProvider)

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatioCustom(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()

            imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatioCustom(screenAspectRatio)
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setAnalyzer(backgroundExecutor, imageAnalyzer)
                }

            cameraProvider.unbindAll()

            try {
                camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis
                )
            } catch (e: Exception) {
                Crashlytics.log(Log.ERROR, CRASHLYTICS_CAMERAX, "Use case binding failed, $e")
            }
        }, mainExecutor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        displayManager.unregisterDisplayListener(displayListener)
    }

    private val imageSavedListener = object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            imageCaptureFile?.let { uri ->
                openEdit(uri.toString(), false)
            }
        }

        override fun onError(exception: ImageCaptureException) {
            context?.toast("Photo capture failed: ${exception.message}")
            Crashlytics.log(Log.ERROR, CRASHLYTICS_CAMERAX, "Photo capture failed: ${exception.message}")
        }
    }

    private val isGooglePlayServicesAvailable by lazy {
        context?.isGooglePlayServicesAvailable() ?: false
    }

    private val imageAnalyzer = object : ImageAnalysis.Analyzer {
        private val detecting = AtomicBoolean(false)

        override fun analyze(image: ImageProxy) {
            if (!alreadyDetected && !image.planes.isNullOrEmpty() &&
                detecting.compareAndSet(false, true)
            ) {
                if (isGooglePlayServicesAvailable) {
                    decodeWithFirebaseVision(image)
                } else {
                    decodeWithZxing(image)
                }
            } else {
                image.close()
            }
        }

        private fun decodeWithFirebaseVision(image: ImageProxy) {
            val processImage = image.image
            if (processImage == null) {
                image.close()
                return
            }
            val visionImage = FirebaseVisionImage.fromMediaImage(
                processImage,
                FirebaseVisionImageMetadata.ROTATION_90
            )
            val latch = CountDownLatch(1)
            detector.use { d ->
                d.detectInImage(visionImage)
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
                    if (!isAdded) return@launch
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
                    if (!isAdded) return@launch
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
                Crashlytics.log(Log.ERROR, CRASHLYTICS_CAMERAX, "getBitmapFromImage failure, $e")
                null
            }
        }
    }
}
