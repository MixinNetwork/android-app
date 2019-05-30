package one.mixin.android.ui.qr

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.util.DisplayMetrics
import android.util.Rational
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysisConfig
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureConfig
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.PreviewConfig
import androidx.core.view.isVisible
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.tbruyelle.rxpermissions2.RxPermissions
import kotlinx.android.synthetic.main.fragment_capture.*
import kotlinx.android.synthetic.main.view_camera_tip.view.*
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.bounce
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getImageCachePath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.hasNavigationBar
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.openGallery
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.widget.CameraOpView
import one.mixin.android.widget.gallery.ui.GalleryActivity
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class CaptureFragment : BaseCaptureFragment() {
    companion object {
        const val TAG = "CaptureFragment"

        const val SHOW_QR_CODE = "show_qr_code"

        const val ARGS_FOR_ADDRESS = "args_for_address"
        const val ARGS_ADDRESS_RESULT = "args_address_result"
        const val ARGS_FOR_ACCOUNT_NAME = "args_for_account_name"
        const val ARGS_ACCOUNT_NAME_RESULT = "args_account_name_result"
        const val REQUEST_CODE = 0x0000c0ff
        const val RESULT_CODE = 0x0000c0df

        val SCOPES = arrayListOf("PROFILE:READ", "PHONE:READ", "ASSETS:READ", "APPS:READ", "APPS:WRITE", "CONTACTS:READ")

        private const val MAX_DURATION = 15
        private const val MIN_DURATION = 1

        fun newInstance(forAddress: Boolean = false, forAccountName: Boolean = false) = CaptureFragment().withArgs {
            putBoolean(ARGS_FOR_ADDRESS, forAddress)
            putBoolean(ARGS_FOR_ACCOUNT_NAME, forAccountName)
        }
    }

    private val forAddress: Boolean by lazy { arguments!!.getBoolean(ARGS_FOR_ADDRESS) }
    private val forAccountName: Boolean by lazy { arguments!!.getBoolean(ARGS_FOR_ACCOUNT_NAME) }

    private var lensFacing = CameraX.LensFacing.BACK

    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null

    private var alreadyDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_capture, container, false)

    @SuppressLint("RestrictedApi", "AutoDispose")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!defaultSharedPreferences.getBoolean(Constants.Account.PREF_CAMERA_TIP, false)) {
            val v = stub.inflate()
            v.continue_tv.setOnClickListener {
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_CAMERA_TIP, true)
                v.visibility = GONE
            }
        }
        op.post {
            if (!isAdded) return@post

            val b = bottom_ll.bottom
            val hasNavigationBar = requireContext().hasNavigationBar(b)
            if (hasNavigationBar) {
                val navigationBarHeight = requireContext().navigationBarHeight()
                bottom_ll.translationY = -navigationBarHeight.toFloat()
            }
        }
        close.setOnClickListener { activity?.onBackPressed() }
        flash.setOnClickListener {
            if (preview?.isTorchOn == true) {
                flash.setImageResource(R.drawable.ic_flash_off)
                preview?.enableTorch(false)
            } else {
                flash.setImageResource(R.drawable.ic_flash_on)
                preview?.enableTorch(true)
            }
            flash.bounce()
        }
        close.setOnClickListener { activity?.onBackPressed() }
        switch_camera.setOnClickListener {
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
            checkFlash()
            switch_camera.bounce()
        }
        gallery_iv.setOnClickListener {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({ granted ->
                    if (granted) {
                        openGallery()
                    } else {
                        context?.openPermissionSetting()
                    }
                }, {
                })
        }
        checkFlash()
        op.setMaxDuration(MAX_DURATION)
        op.setCameraOpCallback(opCallback)
        view_finder.post {
            bindCameraUseCase()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let {
                val path = it.getFilePath(MixinApplication.get())
                if (path == null) {
                    context?.toast(R.string.error_image)
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

    private fun checkFlash() {
        if (CameraX.LensFacing.BACK == lensFacing) {
            flash.visibility = VISIBLE
        } else {
            flash.visibility = GONE
        }
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

    private fun handleAnalysis(analysisResult: String) {
        if (!isAdded) return

        requireContext().defaultSharedPreferences.putBoolean(SHOW_QR_CODE, false)
        if (forAddress || forAccountName) {
            val result = Intent().apply {
                putExtra(if (forAddress) ARGS_ADDRESS_RESULT else ARGS_ACCOUNT_NAME_RESULT, analysisResult)
            }
            activity?.setResult(RESULT_CODE, result)
            activity?.finish()
            return
        }
        if (analysisResult.startsWith(Constants.Scheme.DEVICE)) {
            val confirmBottomFragment = ConfirmBottomFragment.newInstance(analysisResult)
            confirmBottomFragment.setCallBack {
                activity?.finish()
            }
            confirmBottomFragment.show(fragmentManager, ConfirmBottomFragment.TAG)
        } else {
            pseudoNotificationView.addContent(analysisResult)
        }
    }

    private fun openEdit(path: String, isVideo: Boolean, fromGallery: Boolean = false) {
        activity?.supportFragmentManager?.inTransaction {
            add(R.id.container, EditFragment.newInstance(path, isVideo, fromGallery), EditFragment.TAG)
                .addToBackStack(null)
        }
    }

    private val opCallback = object : CameraOpView.CameraOpCallback {
        val audioManager by lazy { requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        var oldStreamVolume = 0
        override fun onClick() {
            val outFile = requireContext().getImageCachePath().createImageTemp()
            val metadata = ImageCapture.Metadata().apply {
                isReversedHorizontal = lensFacing == CameraX.LensFacing.FRONT
            }
            imageCapture?.takePicture(outFile, imageSavedListener, metadata)
            capture_border_view?.isVisible = true
            capture_border_view?.postDelayed({
                capture_border_view?.isVisible = false
            }, 100)
        }

        private var videoFile: File? = null

        override fun onProgressStart() {
            close.fadeOut()
            flash.fadeOut()
            switch_camera.fadeOut()
            chronometer_layout.fadeIn()
            chronometer.base = SystemClock.elapsedRealtime()
            chronometer.start()
            videoFile = requireContext().getVideoPath().createVideoTemp("mp4")
            try {
                oldStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            } catch (ignored: SecurityException) {
            }
        }

        override fun onProgressStop(time: Float) {
            close.fadeIn()
            flash.fadeIn()
            switch_camera.fadeIn()
            chronometer_layout.fadeOut()
            chronometer.stop()
            if (time < MIN_DURATION) {
                toast(R.string.error_duration_short)
            } else {
                videoFile?.let {
                    activity?.supportFragmentManager?.inTransaction {
                        add(R.id.container, EditFragment.newInstance(it.absolutePath, true), EditFragment.TAG)
                            .addToBackStack(null)
                    }
                }
            }
            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.N && Build.VERSION.SDK_INT != Build.VERSION_CODES.N_MR1) {
                requireContext().mainThreadDelayed({
                    audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, oldStreamVolume, 0)
                }, 300)
            }
        }
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

    private val imageAnalyzer = object : ImageAnalysis.Analyzer {
        private val detecting = AtomicBoolean(false)

        override fun analyze(image: ImageProxy, rotationDegrees: Int) {
            if (!alreadyDetected && !image.planes.isNullOrEmpty() && detecting.compareAndSet(false, true)) {
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
        }
    }
}