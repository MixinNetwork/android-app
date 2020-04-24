package one.mixin.android.ui.qr

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.TorchState
import androidx.camera.core.UseCase
import androidx.camera.core.impl.utils.futures.FutureCallback
import androidx.camera.core.impl.utils.futures.Futures
import androidx.lifecycle.lifecycleScope
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.android.synthetic.main.fragment_scan.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.decodeQR
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.isFirebaseDecodeAvailable
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.util.reportException
import org.jetbrains.anko.getStackTraceString
import timber.log.Timber

class ScanFragment : BaseCameraxFragment() {
    companion object {
        const val TAG = "ScanFragment"

        fun newInstance(
            forAddress: Boolean = false,
            forAccountName: Boolean = false,
            forMemo: Boolean = false
        ) = ScanFragment().withArgs {
            putBoolean(CaptureActivity.ARGS_FOR_ADDRESS, forAddress)
            putBoolean(CaptureActivity.ARGS_FOR_ACCOUNT_NAME, forAccountName)
            putBoolean(CaptureActivity.ARGS_FOR_MEMO, forMemo)
        }
    }

    private val forAddress: Boolean by lazy { requireArguments().getBoolean(CaptureActivity.ARGS_FOR_ADDRESS) }
    private val forAccountName: Boolean by lazy { requireArguments().getBoolean(CaptureActivity.ARGS_FOR_ACCOUNT_NAME) }
    private val forMemo: Boolean by lazy { requireArguments().getBoolean(CaptureActivity.ARGS_FOR_MEMO) }

    private var alreadyDetected = false

    private var imageAnalysis: ImageAnalysis? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_scan, container, false)

    @SuppressLint("RestrictedApi")
    override fun onFlashClick() {
        if (camera?.cameraInfo?.hasFlashUnit() == false) {
            toast(R.string.no_flash_unit)
            return
        }
        val torchState = camera?.cameraInfo?.torchState?.value ?: TorchState.OFF
        flash.setImageResource(R.drawable.ic_scan_flash)
        val future = (if (torchState == TorchState.ON) {
            camera?.cameraControl?.enableTorch(false)
        } else {
            camera?.cameraControl?.enableTorch(true)
        }) ?: return
        Futures.addCallback(future, object : FutureCallback<Void> {
            override fun onSuccess(result: Void?) {
            }

            override fun onFailure(t: Throwable?) {
                Timber.d("enableTorch onFailure, ${t?.getStackTraceString()}")
            }
        }, mainExecutor)
    }

    @SuppressLint("RestrictedApi")
    override fun getOtherUseCases(
        screenAspectRatio: Rational,
        rotation: Int
    ): Array<UseCase> {
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatioCustom(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setAnalyzer(backgroundExecutor, imageAnalyzer)
            }
        return arrayOf(imageAnalysis!!)
    }

    override fun onDisplayChanged(rotation: Int) {
        imageAnalysis?.targetRotation = rotation
    }

    override fun needScan() = true

    private fun handleAnalysis(analysisResult: String) {
        if (!isAdded) return

        requireContext().defaultSharedPreferences.putBoolean(CaptureActivity.SHOW_QR_CODE, false)
        if (forAddress || forAccountName || forMemo) {
            val result = Intent().apply {
                putExtra(when {
                    forAddress -> CaptureActivity.ARGS_ADDRESS_RESULT
                    forAccountName -> CaptureActivity.ARGS_ACCOUNT_NAME_RESULT
                    else -> CaptureActivity.ARGS_MEMO_RESULT
                }, analysisResult)
            }
            activity?.setResult(CaptureActivity.RESULT_CODE, result)
            activity?.finish()
            return
        }
        if (analysisResult.startsWith(Constants.Scheme.DEVICE)) {
            ConfirmBottomFragment.show(requireContext(), parentFragmentManager, analysisResult) {
                activity?.finish()
            }
        } else {
            handleResult(analysisResult)
        }
    }

    private val isGooglePlayServicesAvailable by lazy {
        context?.isFirebaseDecodeAvailable() ?: false
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

        @SuppressLint("UnsafeExperimentalUsageError")
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
                reportException("$CRASHLYTICS_CAMERAX-getBitmapFromImage failure", e)
                null
            }
        }
    }
}
