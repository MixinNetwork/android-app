package one.mixin.android.ui.qr

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.TorchState
import androidx.camera.core.UseCase
import androidx.core.view.isVisible
import com.crashlytics.android.Crashlytics
import java.io.File
import kotlinx.android.synthetic.main.fragment_capture.*
import one.mixin.android.R
import one.mixin.android.extension.bounce
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getImageCachePath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.hasNavigationBar
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.toast
import one.mixin.android.widget.CameraOpView

class CaptureFragment : BaseCameraxFragment() {
    companion object {
        const val TAG = "CaptureFragment"

        fun newInstance() = CaptureFragment()
    }

    private var imageCaptureFile: File? = null

    private var imageCapture: ImageCapture? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        layoutInflater.inflate(R.layout.fragment_capture, container, false)

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        op.post {
            if (!isAdded) return@post

            val b = bottom_ll.bottom
            val hasNavigationBar = requireContext().hasNavigationBar(b)
            if (hasNavigationBar) {
                val navigationBarHeight = requireContext().navigationBarHeight()
                bottom_ll.translationY = -navigationBarHeight.toFloat()
            }
        }
        switch_camera.setOnClickListener {
            onSwitchClick()
            checkFlash()
            switch_camera.bounce()
        }
        op.setMaxDuration(CaptureActivity.MAX_DURATION)
        op.setCameraOpCallback(opCallback)
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
    override fun getOtherUseCases(screenAspectRatio: Rational, rotation: Int): Array<UseCase> {
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatioCustom(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
        return arrayOf(imageCapture!!)
    }

    override fun onDisplayChanged(rotation: Int) {
        imageCapture?.targetRotation = rotation
    }

    @SuppressLint("RestrictedApi")
    private fun onSwitchClick() {
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

    private val opCallback = object : CameraOpView.CameraOpCallback {
        val audioManager by lazy { requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager }
        var oldStreamVolume = 0
        override fun onClick() {
            onTakePicture()
            capture_border_view?.isVisible = true
            capture_border_view?.postDelayed({
                capture_border_view?.isVisible = false
            }, 100)
        }

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
            onRecordStart()
        }

        override fun onProgressStop(time: Float) {
            close.fadeIn()
            flash.fadeIn()
            switch_camera.fadeIn()
            chronometer_layout.fadeOut()
            chronometer.stop()
            if (time < CaptureActivity.MIN_DURATION) {
                onStopAndResume()
                toast(R.string.error_duration_short)
            } else {
                videoFile?.let {
                    onStopAndPause()
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

    private fun onStopAndPause() {
    }

    private fun onStopAndResume() {
    }

    private fun onRecordStart() {
    }
}
