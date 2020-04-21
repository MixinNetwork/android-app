package one.mixin.android.ui.qr

import android.net.Uri
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import one.mixin.android.Constants
import one.mixin.android.extension.closeSilently
import one.mixin.android.extension.isMixinUrl
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity

abstract class VisionFragment : BaseFragment() {

    protected val detector: FirebaseVisionBarcodeDetector =
        FirebaseVision.getInstance().getVisionBarcodeDetector(
            FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
                .build()
        )

    override fun onDestroyView() {
        super.onDestroyView()
        detector.closeSilently()
    }

    protected fun handleResult(content: String) {
        if (!content.isMixinUrl()) {
            MainActivity.showFromScan(requireActivity(), scanText = content)
        } else if (content.startsWith(Constants.Scheme.TRANSFER, true) ||
            content.startsWith(Constants.Scheme.HTTPS_TRANSFER, true)
        ) {
            val segments = Uri.parse(content).pathSegments
            val userId = if (segments.size >= 2) {
                segments[1]
            } else {
                segments[0]
            }
            MainActivity.showFromScan(requireActivity(), userId = userId)
        } else {
            MainActivity.showFromScan(requireActivity(), url = content)
        }
    }
}
