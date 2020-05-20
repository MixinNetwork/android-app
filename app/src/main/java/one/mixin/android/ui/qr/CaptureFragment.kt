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
import androidx.core.view.isVisible
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
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
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.util.reportException
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
        val flashMode = imageCapture?.flashMode ?: FLASH_MODE_OFF
        if (flashMode == FLASH_MODE_ON) {
            flash.setImageResource(R.drawable.ic_flash_off)
            imageCapture?.flashMode = FLASH_MODE_OFF
        } else {
            flash.setImageResource(R.drawable.ic_flash_on)
            imageCapture?.flashMode = FLASH_MODE_ON
        }
    }

    @SuppressLint("RestrictedApi")
    override fun getOtherUseCases(
        rotation: Int
    ): Array<UseCase> {
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetResolution(Size(metrics.widthPixels, metrics.heightPixels))
            .setTargetRotation(rotation)
            .build()
        return arrayOf(imageCapture!!)
    }

    override fun onDisplayChanged(rotation: Int) {
        imageCapture?.targetRotation = rotation
    }

    override fun fromScan() = false

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
                .subscribe({ granted ->
                    if (granted) {
                        onTakePicture()
                        capture_border_view?.isVisible = true
                        capture_border_view?.postDelayed({
                            capture_border_view?.isVisible = false
                        }, 100)
                    } else {
                        context?.openPermissionSetting()
                    }
                }, {
                })
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
            videoFile?.let {
//                view_finder.startRecording(it, backgroundExecutor, onVideoSavedCallback)
            }
        }

        override fun onProgressStop(time: Float) {
            close.fadeIn()
            flash.fadeIn()
            switch_camera.fadeIn()
            chronometer_layout.fadeOut()
            chronometer.stop()
            if (time < CaptureActivity.MIN_DURATION) {
                toast(R.string.error_duration_short)
            } else {
                videoFile?.let {
//                    view_finder.stopRecording()
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
}
