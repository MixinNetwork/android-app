package one.mixin.android.ui.qr

import android.graphics.BitmapFactory
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.CaptureManagerCallback
import com.journeyapps.barcodescanner.Size
import com.journeyapps.barcodescanner.SourceData
import kotlinx.android.synthetic.main.fragment_capture_camerax.flash
import kotlinx.android.synthetic.main.fragment_capture_zxing.*
import kotlinx.android.synthetic.main.view_custom_barcode_scannner.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import one.mixin.android.R
import one.mixin.android.extension.closeSilently
import one.mixin.android.extension.createImageTemp
import one.mixin.android.extension.getImageCachePath
import one.mixin.android.extension.mainThreadDelayed
import one.mixin.android.extension.rotate
import one.mixin.android.extension.toBytes
import one.mixin.android.extension.withArgs
import one.mixin.android.extension.xYuv2Simple
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_ACCOUNT_NAME
import one.mixin.android.ui.qr.CaptureActivity.Companion.ARGS_FOR_ADDRESS
import one.mixin.android.ui.qr.CaptureActivity.Companion.MAX_DURATION
import java.io.FileOutputStream

class ZxingCaptureFragment : BaseCaptureFragment() {
    companion object {
        const val TAG = "ZxingCaptureFragment"

        fun newInstance(forAddress: Boolean = false, forAccountName: Boolean = false) = ZxingCaptureFragment().withArgs {
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

    private var mode = Mode.SCAN
    private var flashOpen = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        layoutInflater.inflate(R.layout.fragment_capture_zxing, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mCaptureManager.initializeFromIntent(activity!!.intent, savedInstanceState)
        mCaptureManager.decode()

        val p = Point()
        activity?.windowManager?.defaultDisplay?.getSize(p)
        zxing_barcode_surface.framingRectSize = Size(p.x, p.y)
    }

    override fun onFlashClick() {
        if (flashOpen) {
            flash.setImageResource(R.drawable.ic_flash_off)
            zxing_barcode_scanner.setTorchOff()
        } else {
            flash.setImageResource(R.drawable.ic_flash_on)
            zxing_barcode_scanner.setTorchOn()
        }

        flashOpen = !flashOpen
    }

    override fun onSwitchClick() {
        zxing_barcode_scanner.switchCamera()
    }

    override fun isLensBack() = zxing_barcode_scanner.isFacingBack

    override fun onTakePicture() {
        mCaptureManager.capture()
        mode = Mode.CAPTURE
    }

    override fun onRecordStart() {
        mCaptureManager.record(videoFile, MAX_DURATION)
        mode = Mode.RECORD
    }

    override fun onStopAndResume() {
        mCaptureManager.stopRecord()
        mCaptureManager.resume()
        mode = Mode.SCAN
    }

    override fun onStopAndPause() {
        mCaptureManager.stopRecord()
        mCaptureManager.pause()
        mode = Mode.SCAN
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

    override fun afterPseudoClick() {
        mCaptureManager.closeAndFinish()
    }

    private fun handleCapture(sourceData: SourceData) {
        val imageBytes = sourceData.data.xYuv2Simple(sourceData.dataWidth, sourceData.dataHeight)
        val rawBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        val bitmap = rawBitmap.rotate(sourceData.dataWidth, sourceData.dataHeight,
            sourceData.rotation, zxing_barcode_scanner.isFacingBack)
        val outFile = context!!.getImageCachePath().createImageTemp()
        lifecycleScope.launch(Dispatchers.IO) {
            val out = FileOutputStream(outFile)
            out.write(bitmap.toBytes())
            out.closeSilently()

            openEdit(outFile.absolutePath, isVideo = false)
        }
    }

    override fun afterSetPseudoView() {
        requireContext().mainThreadDelayed({
            mCaptureManager.decode()
        }, 1000)
    }

    private val captureCallback = object : CaptureManagerCallback {
        override fun onScanResult(barcodeResult: BarcodeResult) {
            handleAnalysis(barcodeResult.text)
        }

        override fun onPreview(sourceData: SourceData) {
            if (mode == Mode.CAPTURE) {
                handleCapture(sourceData)
                mCaptureManager.pause()
            } else if (mode == Mode.RECORD) {
            }
        }
    }
}
