package one.mixin.android.ui.qr

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import android.view.View
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.crashlytics.android.Crashlytics
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlinx.android.synthetic.main.fragment_capture.*
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.bounce
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.openGallery
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.toast
import one.mixin.android.widget.gallery.ui.GalleryActivity

abstract class BaseCameraxFragment : VisionFragment() {
    companion object {
        const val CRASHLYTICS_CAMERAX = "camerax"
    }

    protected var videoFile: File? = null

    protected var lensFacing = CameraSelector.LENS_FACING_BACK

    private var preview: Preview? = null
    protected lateinit var mainExecutor: Executor
    protected lateinit var backgroundExecutor: Executor
    protected var camera: Camera? = null

    private var displayId: Int = -1
    private lateinit var displayManager: DisplayManager

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) = view?.let { view ->
            if (displayId == this@BaseCameraxFragment.displayId) {
                this@BaseCameraxFragment.onDisplayChanged(view.display.rotation)
            }
        } ?: Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainExecutor = ContextCompat.getMainExecutor(requireContext())
        backgroundExecutor = Executors.newSingleThreadExecutor()
    }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        close.setOnClickListener { activity?.onBackPressed() }
        flash.setOnClickListener {
            onFlashClick()
            flash.bounce()
        }
        gallery_iv.setOnClickListener {
            RxPermissions(requireActivity())
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .autoDispose(stopScope)
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

        displayManager = view_finder.context
            .getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, null)

        view_finder.post {
            displayId = view_finder.display.displayId
            bindCameraUseCase()
        }
    }

    @SuppressLint("RestrictedApi")
    protected fun bindCameraUseCase() {
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

            val otherUseCases = getOtherUseCases(screenAspectRatio, rotation)

            cameraProvider.unbindAll()

            try {
                camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner, cameraSelector, preview, *otherUseCases
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

    private fun isLensBack() = CameraSelector.LENS_FACING_BACK == lensFacing

    protected fun openEdit(path: String, isVideo: Boolean, fromGallery: Boolean = false) {
        activity?.supportFragmentManager?.inTransaction {
            add(R.id.container, EditFragment.newInstance(path, isVideo, fromGallery), EditFragment.TAG)
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

    abstract fun onFlashClick()
    abstract fun getOtherUseCases(screenAspectRatio: Rational, rotation: Int): Array<UseCase>
    abstract fun onDisplayChanged(rotation: Int)
}
