package one.mixin.android.ui.qr

import android.net.Uri
import android.os.Bundle
import android.view.View
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.closeSilently
import one.mixin.android.extension.dpToPx
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.ui.url.isMixinUrl
import one.mixin.android.widget.PseudoNotificationView

abstract class CaptureVisionFragment : BaseFragment() {
    protected lateinit var pseudoNotificationView: PseudoNotificationView

    protected val detector: FirebaseVisionBarcodeDetector =
        FirebaseVision.getInstance().getVisionBarcodeDetector(
            FirebaseVisionBarcodeDetectorOptions.Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
                .build()
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pseudoNotificationView = view.findViewById<PseudoNotificationView>(R.id.pseudo_view).apply {
            translationY = -requireContext().dpToPx(300f).toFloat()
            callback = pseudoViewCallback
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detector.closeSilently()
    }

    private val pseudoViewCallback = object : PseudoNotificationView.Callback {
        override fun onClick(content: String) {
            if (!isMixinUrl(content)) {
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
}
