package one.mixin.android.ui.qr

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig.fromOrigamiTensionAndFriction
import com.facebook.rebound.SpringSystem
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.CaptureManagerCallback
import com.journeyapps.barcodescanner.Size
import com.journeyapps.barcodescanner.SourceData
import kotlinx.android.synthetic.main.fragment_capture.*
import kotlinx.android.synthetic.main.view_camera_tip.view.*
import kotlinx.android.synthetic.main.view_custom_barcode_scannner.*
import one.mixin.android.Constants
import one.mixin.android.Constants.Scheme
import one.mixin.android.R
import one.mixin.android.extension.closeSilently
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.createVideoTemp
import one.mixin.android.extension.defaultSharedPreferences
import one.mixin.android.extension.fadeIn
import one.mixin.android.extension.fadeOut
import one.mixin.android.extension.getImageCachePath
import one.mixin.android.extension.getVideoPath
import one.mixin.android.extension.hasNavigationBar
import one.mixin.android.extension.inTransaction
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.navigationBarHeight
import one.mixin.android.extension.putBoolean
import one.mixin.android.extension.rotate
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.toast
import one.mixin.android.extension.withArgs
import one.mixin.android.extension.xYuv2Simple
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.url.isMixinUrl
import one.mixin.android.widget.AndroidUtilities.dp
import one.mixin.android.widget.CameraOpView
import one.mixin.android.widget.PseudoNotificationView
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.FileOutputStream

class CaptureFragment : BaseFragment() {
    companion object {
        const val TAG = "CaptureFragment"

        const val SHOW_QR_CODE = "show_qr_code"

        const val ARGS_FOR_ADDRESS = "args_for_address"
        const val ARGS_ADDRESS_RESULT = "args_address_result"
        const val ARGS_FOR_ACCOUNT_NAME = "args_for_account_name"
        const val ARGS_ACCOUNT_NAME_RESULT = "args_account_name_result"
        const val RESULT_CODE = 0x0000c0df

        val SCOPES = arrayListOf("PROFILE:READ", "PHONE:READ", "ASSETS:READ", "APPS:READ", "APPS:WRITE", "CONTACTS:READ")

        private const val MAX_DURATION = 15
        private const val MIN_DURATION = 1

        fun newInstance(forAddress: Boolean = false, forAccountName: Boolean = false) = CaptureFragment().withArgs {
            putBoolean(ARGS_FOR_ADDRESS, forAddress)
            putBoolean(ARGS_FOR_ACCOUNT_NAME, forAccountName)
        }
    }

    private enum class Mode {
        SCAN,
        CAPTURE,
        RECORD
    }

    private val mCaptureManager: CaptureManager by lazy {
        CaptureManager(activity, zxing_barcode_scanner, captureCallback)
    }

    private val springSystem = SpringSystem.create()
    private val sprintConfig = fromOrigamiTensionAndFriction(80.0, 4.0)

    private val forAddress: Boolean by lazy { arguments!!.getBoolean(ARGS_FOR_ADDRESS) }
    private val forAccountName: Boolean by lazy { arguments!!.getBoolean(ARGS_FOR_ACCOUNT_NAME) }

    private var mode = Mode.SCAN
    private var flashOpen = false

    private var callback: Callback? = null

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context !is Callback) {
            throw IllegalArgumentException("")
        }
        callback = context
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_capture, container, false)

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
            val b = op.bottom
            val hasNavigationBar = context!!.hasNavigationBar(b)
            if (hasNavigationBar) {
                val navigationBarHeight = context!!.navigationBarHeight()
                op.translationY = -navigationBarHeight.toFloat()
            }
        }
        close.setOnClickListener { activity?.onBackPressed() }
        mCaptureManager.initializeFromIntent(activity!!.intent, savedInstanceState)
        mCaptureManager.decode()
        flash.setOnClickListener {
            if (flashOpen) {
                flash.setImageResource(R.drawable.ic_flash_off)
                zxing_barcode_scanner.setTorchOff()
            } else {
                flash.setImageResource(R.drawable.ic_flash_on)
                zxing_barcode_scanner.setTorchOn()
            }

            anim(flash)
            flashOpen = !flashOpen
        }
        close.setOnClickListener { activity?.onBackPressed() }
        switch_camera.setOnClickListener {
            anim(switch_camera)
            zxing_barcode_scanner.switchCamera()
            checkFlash()
        }
        checkFlash()
        op.setMaxDuration(MAX_DURATION)
        op.setCameraOpCallback(object : CameraOpView.CameraOpCallback {
            val audioManager by lazy { requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager }
            var oldStreamVolume = 0
            override fun onClick() {
                mCaptureManager.capture()
                mode = Mode.CAPTURE
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
                if (Build.VERSION.SDK_INT != Build.VERSION_CODES.N && Build.VERSION.SDK_INT != Build.VERSION_CODES.N_MR1) {
                    oldStreamVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                    audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0)
                }
                mCaptureManager.record(videoFile, MAX_DURATION)
                mode = Mode.RECORD
            }

            override fun onProgressStop(time: Float) {
                close.fadeIn()
                flash.fadeIn()
                switch_camera.fadeIn()
                chronometer_layout.fadeOut()
                chronometer.stop()
                if (time < MIN_DURATION) {
                    mCaptureManager.stopRecord()
                    mCaptureManager.resume()
                    mode = Mode.SCAN
                    toast(R.string.error_duration_short)
                } else {
                    videoFile?.let {
                        mCaptureManager.stopRecord()
                        mCaptureManager.pause()
                        mode = Mode.SCAN
                        activity?.supportFragmentManager?.inTransaction {
                            add(R.id.container, EditFragment.newInstance(it.absolutePath, true), EditFragment.TAG)
                                .addToBackStack(null)
                        }
                    }
                }
                if (Build.VERSION.SDK_INT != Build.VERSION_CODES.N && Build.VERSION.SDK_INT != Build.VERSION_CODES.N_MR1) {
                    requireContext().mainThreadDelayed({ audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, oldStreamVolume, 0) }, 300)
                }
            }
        })
        pseudo_view.translationY = -dp(300f).toFloat()
        pseudo_view.callback = pseudoViewCallback

        val p = Point()
        activity?.windowManager?.defaultDisplay?.getSize(p)
        zxing_barcode_surface.framingRectSize = Size(p.x, p.y)
    }

    private fun checkFlash() {
        if (zxing_barcode_scanner.isFacingBack) {
            flash.visibility = VISIBLE
        } else {
            flash.visibility = GONE
        }
    }

    private fun anim(view: View) {
        val spring = springSystem.createSpring()
            .setSpringConfig(sprintConfig)
            .addListener(object : SimpleSpringListener() {
                override fun onSpringUpdate(spring: Spring) {
                    val value = spring.currentValue.toFloat()
                    view.scaleX = value
                    view.scaleY = value
                }
            })
        spring.endValue = 1.0
    }

    override fun onResume() {
        super.onResume()
        mCaptureManager.onResume()
    }

    override fun onPause() {
        super.onPause()
        mCaptureManager.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCaptureManager.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mCaptureManager.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        mCaptureManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun resume() {
        mCaptureManager.resume()
    }

    fun handleScanResult(data: String) {
        if (!isMixinUrl(data)) {
            MainActivity.showScan(requireContext(), data)
        } else if (data.startsWith(Scheme.TRANSFER, true) ||
            data.startsWith(Scheme.HTTPS_TRANSFER, true)) {
            val segments = Uri.parse(data).pathSegments
            val userId = if (segments.size >= 2) {
                segments[1]
            } else {
                segments[0]
            }
            MainActivity.showTransfer(requireContext(), userId)
        } else {
            MainActivity.showUrl(requireContext(), data)
        }
        mCaptureManager.closeAndFinish()
    }

    private fun handleCapture(sourceData: SourceData) {
        val imageBytes = sourceData.data.xYuv2Simple(sourceData.dataWidth, sourceData.dataHeight)
        val rawBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val bitmap = rawBitmap.rotate(sourceData.dataWidth, sourceData.dataHeight,
            sourceData.rotation, zxing_barcode_scanner.isFacingBack)
        val outFile = context!!.getImageCachePath().createImageTemp()
        doAsync {
            val out = FileOutputStream(outFile)
            out.write(bitmap.toBytes())
            out.closeSilently()
        }

        callback?.setBitmap(bitmap)
        activity?.supportFragmentManager?.inTransaction {
            add(R.id.container, EditFragment.newInstance(outFile.absolutePath), EditFragment.TAG)
                .addToBackStack(null)
        }
    }

    private val pseudoViewCallback = object : PseudoNotificationView.Callback {
        override fun onClick(content: String) {
            handleScanResult(content)
        }
    }

    private val captureCallback = object : CaptureManagerCallback {
        override fun onScanResult(barcodeResult: BarcodeResult) {
            requireContext().defaultSharedPreferences.putBoolean(SHOW_QR_CODE, false)
            if (forAddress || forAccountName) {
                val result = Intent().apply {
                    putExtra(if (forAddress) ARGS_ADDRESS_RESULT else ARGS_ACCOUNT_NAME_RESULT, barcodeResult.text)
                }
                activity?.setResult(RESULT_CODE, result)
                activity?.finish()
                return
            }
            pseudo_view.addContent(barcodeResult.text)
            requireContext().mainThreadDelayed({
                mCaptureManager.decode()
            }, 1000)
        }

        override fun onPreview(sourceData: SourceData) {
            if (mode == Mode.CAPTURE) {
                handleCapture(sourceData)
                mCaptureManager.pause()
            } else if (mode == Mode.RECORD) {
            }
        }
    }

    interface Callback {
        fun setBitmap(bitmap: Bitmap)
    }
}