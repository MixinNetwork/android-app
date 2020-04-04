package one.mixin.android.ui.qr

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.core.view.isVisible
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uber.autodispose.autoDispose
import java.io.File
import kotlinx.android.synthetic.main.fragment_capture_camerax.*
import kotlinx.android.synthetic.main.view_camera_tip.view.*
import one.mixin.android.Constants
import one.mixin.android.MixinApplication
import one.mixin.android.R
import one.mixin.android.extension.REQUEST_GALLERY
import one.mixin.android.extension.bounce
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getFilePath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.hasNavigationBar
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.openGallery
import one.mixin.android.extension.openPermissionSetting
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.toast
import one.mixin.android.ui.device.ConfirmBottomFragment
import one.mixin.android.widget.CameraOpView
import one.mixin.android.widget.gallery.ui.GalleryActivity

abstract class BaseCaptureFragment : CaptureVisionFragment() {

    protected var videoFile: File? = null

    private val forAddress: Boolean by lazy { requireArguments().getBoolean(CaptureActivity.ARGS_FOR_ADDRESS) }
    private val forAccountName: Boolean by lazy { requireArguments().getBoolean(CaptureActivity.ARGS_FOR_ACCOUNT_NAME) }
    private val forMemo: Boolean by lazy { requireArguments().getBoolean(CaptureActivity.ARGS_FOR_MEMO) }

    @SuppressLint("RestrictedApi")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!defaultSharedPreferences.getBoolean(Constants.Account.PREF_CAMERA_TIP, false)) {
            val v = stub.inflate()
            v.continue_tv.setOnClickListener {
                defaultSharedPreferences.putBoolean(Constants.Account.PREF_CAMERA_TIP, true)
                v.visibility = View.GONE
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
            onFlashClick()
            flash.bounce()
        }
        close.setOnClickListener { activity?.onBackPressed() }
        switch_camera.setOnClickListener {
            onSwitchClick()
            checkFlash()
            switch_camera.bounce()
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
        op.setMaxDuration(CaptureActivity.MAX_DURATION)
        op.setCameraOpCallback(opCallback)
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

    protected fun openEdit(path: String, isVideo: Boolean, fromGallery: Boolean = false) {
        activity?.supportFragmentManager?.inTransaction {
            add(R.id.container, EditFragment.newInstance(path, isVideo, fromGallery), EditFragment.TAG)
                .addToBackStack(null)
        }
    }

    protected fun handleAnalysis(analysisResult: String) {
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
            pseudoNotificationView.addContent(analysisResult)
            afterSetPseudoView()
        }
    }

    private fun checkFlash() {
        if (isLensBack()) {
            flash.visibility = View.VISIBLE
        } else {
            flash.visibility = View.GONE
        }
    }

    abstract fun onFlashClick()
    abstract fun onSwitchClick()
    abstract fun isLensBack(): Boolean
    abstract fun onTakePicture()

    open fun onRecordStart() {
        // Left empty
    }
    open fun onStopAndResume() {
        // Left empty
    }
    open fun onStopAndPause() {
        // Left empty
    }
    open fun afterSetPseudoView() {
        // Lef empty
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
}
