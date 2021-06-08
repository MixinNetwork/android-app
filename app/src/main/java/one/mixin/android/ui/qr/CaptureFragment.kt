package one.mixin.android.ui.qr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.FLASH_MODE_OFF
import androidx.camera.core.ImageCapture.FLASH_MODE_ON
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.UseCase
import androidx.camera.core.VideoCapture
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.R
import one.mixin.android.databinding.FragmentCaptureBinding
import one.mixin.android.extension.bounce
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.dp
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getImageCachePath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.hasNavigationBar
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.extension.viewDestroyed
import one.mixin.android.util.reportException
import one.mixin.android.util.viewBinding
import one.mixin.android.widget.CameraOpView
import java.io.File
import kotlin.math.max

@AndroidEntryPoint
class CaptureFragment : BaseCameraxFragment() {
    companion object {
        const val TAG = "CaptureFragment"

        fun newInstance() = CaptureFragment()
    }

    private var imageCaptureFile: File? = null

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_capture, container, false)

    private val binding by viewBinding(FragmentCaptureBinding::bind)

    override fun getContentView(): View = binding.root

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.op.post {
            if (viewDestroyed()) return@post

            val b = binding.bottomLl.bottom
            val hasNavigationBar = requireContext().hasNavigationBar(b)
            if (hasNavigationBar) {
                val navigationBarHeight = requireContext().navigationBarHeight()
                binding.bottomLl.translationY = -navigationBarHeight.toFloat()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val insetTop = requireActivity().window.decorView.rootWindowInsets?.displayCutout?.safeInsetTop ?: 0
                val top = binding.chronometerLayout.marginTop
                binding.chronometerLayout.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = max(insetTop + 8.dp, top)
                }
            }
        }
        binding.switchCamera.setOnClickListener {
            onSwitchClick()
            checkFlash()
            binding.switchCamera.bounce()
        }
        binding.op.setMaxDuration(CaptureActivity.MAX_DURATION)
        binding.op.setCameraOpCallback(opCallback)
    }

    override fun onFlashClick() {
        val flashMode = imageCapture?.flashMode ?: FLASH_MODE_OFF
        if (flashMode == FLASH_MODE_ON) {
            binding.flash.setImageResource(R.drawable.ic_flash_off)
            imageCapture?.flashMode = FLASH_MODE_OFF
        } else {
            binding.flash.setImageResource(R.drawable.ic_flash_on)
            imageCapture?.flashMode = FLASH_MODE_ON
        }
    }

    @SuppressLint("RestrictedApi")
    override fun appendOtherUseCases(
        useCases: ArrayList<UseCase>,
        rotation: Int
    ) {
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
            .setTargetRotation(rotation)
            .build()
        useCases.add(imageCapture!!)
    }

    @SuppressLint("RestrictedApi")
    override fun onDisplayChanged(rotation: Int) {
        imageCapture?.targetRotation = rotation
        videoCapture?.setTargetRotation(rotation)
    }

    override fun fromScan() = false

    @SuppressLint("RestrictedApi")
    private fun onSwitchClick() {
        lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            binding.flash.setImageResource(R.drawable.ic_flash_off)
            CameraSelector.LENS_FACING_FRONT
        }
        try {
            bindCameraUseCase()
        } catch (e: Exception) {
            reportException("$CRASHLYTICS_CAMERAX-Switch lens and rebind use cases failure,", e)
        }
    }

    @SuppressLint("RestrictedApi")
    private fun onTakePicture() {
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
    private fun getVideoCapture(): VideoCapture {
        if (videoCapture != null) return videoCapture!!

        videoCapture = VideoCapture.Builder()
            .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
            .build()
        return videoCapture!!
    }

    private val imageSavedListener = object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            imageCaptureFile?.let { uri ->
                openEdit(uri.toString(), false)
            }
        }

        override fun onError(exception: ImageCaptureException) {
            context?.toast("Photo capture failed: ${exception.message}")
            reportException("$CRASHLYTICS_CAMERAX-Photo capture failed,", exception)
        }
    }

    private val opCallback = object : CameraOpView.CameraOpCallback {
        val audioManager by lazy { requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        var oldStreamVolume = 0
        override fun onClick() {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            onTakePicture()
                            binding.captureBorderView.isVisible = true
                            binding.captureBorderView.postDelayed(
                                {
                                    binding.captureBorderView
                                        .isVisible = false
                                },
                                100
                            )
                        } else {
                            context?.openPermissionSetting()
                        }
                    },
                    {
                    }
                )
        }

        override fun readyForProgress() {
            try {
                oldStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
            } catch (ignored: SecurityException) {
            }
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
                .autoDispose(stopScope)
                .subscribe(
                    { granted ->
                        if (granted) {
                            if (binding.op.isRecording()) {
                                startRecord()
                            }
                        } else {
                            context?.openPermissionSetting()
                        }
                    },
                    {
                    }
                )
        }

        @SuppressLint("RestrictedApi")
        override fun onProgressStop(time: Float) {
            binding.close.fadeIn()
            binding.flash.fadeIn()
            binding.switchCamera.fadeIn()
            binding.chronometerLayout.fadeOut()
            binding.chronometer.stop()

            videoCapture?.stopRecording()
            unbindUseCases(getVideoCapture())

            if (Build.VERSION.SDK_INT != Build.VERSION_CODES.N && Build.VERSION.SDK_INT != Build.VERSION_CODES.N_MR1) {
                requireContext().mainThreadDelayed(
                    {
                        try {
                            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, oldStreamVolume, 0)
                        } catch (ignored: SecurityException) {
                        }
                    },
                    300
                )
            }
        }
    }

    @SuppressLint("RestrictedApi", "MissingPermission")
    private fun startRecord() {
        // Stop using an image analysis useCase before starting a video recording
        // because many devices have limited capacity to combine these useCases.
        // https://issuetracker.google.com/issues/180151796
        // https://developer.android.com/reference/android/hardware/camera2/CameraDevice#createCaptureSession(android.hardware.camera2.params.SessionConfiguration)
        stopImageAnalysis()
        val videoCapture = getVideoCapture()
        bindUseCases(videoCapture)
        val videoFile = requireContext().getVideoPath().createVideoTemp("mp4")
        val outputOptions = VideoCapture.OutputFileOptions.Builder(videoFile).build()

        videoCapture.startRecording(
            outputOptions, mainExecutor,
            object : VideoCapture.OnVideoSavedCallback {
                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    if (binding.op.time < CaptureActivity.MIN_DURATION) {
                        toast(R.string.error_duration_short)
                        outputFileResults.savedUri?.path?.apply {
                            File(this).delete()
                        }
                        startImageAnalysis()
                    } else {
                        openEdit(outputFileResults.savedUri?.path ?: "", true, fromGallery = false)
                    }
                    binding.op.time = 0f
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    context?.toast("Video capture failed: $message")
                    reportException(
                        IllegalStateException(
                            "$CRASHLYTICS_CAMERAX-Video capture failed, " +
                                "message: videoCaptureError: $videoCaptureError, $message, cause: $cause"
                        )
                    )
                    startImageAnalysis()
                }
            }
        )

        binding.close.fadeOut()
        binding.flash.fadeOut()
        binding.switchCamera.fadeOut()
        binding.chronometerLayout.fadeIn()
        binding.chronometer.base = SystemClock.elapsedRealtime()
        binding.chronometer.start()
        binding.op.startProgress()
    }
}
