package one.mixin.android.ui.qr

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysisConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureConfig
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.io.File
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

    private var lensFacing = CameraX.LensFacing.BACK

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null

    private var alreadyDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_capture_camerax, container, false)

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view_finder.post {
            bindCameraUseCase()
        }
    }

    override fun onFlashClick() {
        if (preview?.isTorchOn == true) {
            flash.setImageResource(R.drawable.ic_flash_off)
            preview?.enableTorch(false)
        } else {
            flash.setImageResource(R.drawable.ic_flash_on)
            preview?.enableTorch(true)
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onSwitchClick() {
        lensFacing = if (CameraX.LensFacing.FRONT == lensFacing) {
            CameraX.LensFacing.BACK
        } else {
            CameraX.LensFacing.FRONT
        }
        try {
            CameraX.getCameraWithLensFacing(lensFacing)
            bindCameraUseCase()
        } catch (ignored: Exception) {
        }
    }

    override fun isLensBack() = CameraX.LensFacing.BACK == lensFacing

    override fun onTakePicture() {
        val outFile = requireContext().getImageCachePath().createImageTemp()
        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
        }
        imageCapture?.takePicture(outFile, imageSavedListener, metadata)
    }

    private fun bindCameraUseCase() {
        CameraX.unbindAll()
        val metrics = DisplayMetrics().also { view_finder.display.getRealMetrics(it) }
        val screenSize = Size(metrics.widthPixels, metrics.heightPixels)
        val screenAspectRatio = Rational(metrics.widthPixels, metrics.heightPixels)

        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetResolution(screenSize)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(view_finder.display.rotation)
        }.build()
        preview = AutoFitPreviewBuilder.build(previewConfig, view_finder)

        val imageCaptureConfig = ImageCaptureConfig.Builder().apply {
            setLensFacing(lensFacing)
            setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(view_finder.display.rotation)
        }.build()
        imageCapture = ImageCapture(imageCaptureConfig)

        val imageAnalysisConfig = ImageAnalysisConfig.Builder().apply {
            setLensFacing(lensFacing)
            val analysisThread = HandlerThread("ImageAnalysis").apply { start() }
            setCallbackHandler(Handler(analysisThread.looper))
            setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
        }.build()
        val imageAnalysis = ImageAnalysis(imageAnalysisConfig).apply {
            analyzer = imageAnalyzer
        }

        CameraX.bindToLifecycle(this, preview, imageCapture, imageAnalysis)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        preview?.enableTorch(false)
        CameraX.unbindAll()
    }

    private val imageSavedListener = object : ImageCapture.OnImageSavedListener {
        override fun onImageSaved(file: File) {
            openEdit(file.absolutePath, false)
        }

        override fun onError(useCaseError: ImageCapture.UseCaseError, message: String, cause: Throwable?) {
            context?.toast("Photo capture failed: $message")
            cause?.printStackTrace()
        }
    }

    private val isGooglePlayServicesAvailable by lazy {
        context?.isGooglePlayServicesAvailable() ?: false
    }

    private val imageAnalyzer = object : ImageAnalysis.Analyzer {
        private val detecting = AtomicBoolean(false)

        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            if (!alreadyDetected && !image.planes.isNullOrEmpty() && detecting.compareAndSet(false, true)) {
                if (isGooglePlayServicesAvailable) {
                    decodeWithFirebaseVision(image)
                } else {
                    decodeWithZxing(image)
                }
            }
        }

        private fun decodeWithFirebaseVision(image: ImageProxy) {
            val buffer = image.planes[0].buffer
            val imageMetadata = FirebaseVisionImageMetadata.Builder().apply {
                setWidth(image.width)
                setHeight(image.height)
                setRotation(FirebaseVisionImageMetadata.ROTATION_90)
                setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
            }.build()
            val visionImage = FirebaseVisionImage.fromByteBuffer(buffer, imageMetadata)
            detector.use { d ->
                d.detectInImage(visionImage)
                    .addOnSuccessListener { result ->
                        result.firstOrNull()?.rawValue?.let {
                            alreadyDetected = true
                            handleAnalysis(it)
                        }
                    }
                    .addOnCompleteListener {
                        detecting.set(false)
                    }
            }
        }

        private fun decodeWithZxing(imageProxy: ImageProxy) {
            val byteArray = ImageUtil.imageToJpegByteArray(imageProxy)
            val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
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
            detecting.set(false)
        }
    }
}
