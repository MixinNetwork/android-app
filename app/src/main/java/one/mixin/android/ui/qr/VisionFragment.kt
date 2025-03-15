package one.mixin.android.ui.qr

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE
import one.mixin.android.Constants
import one.mixin.android.R
import one.mixin.android.extension.closeSilently
import one.mixin.android.extension.dp
import one.mixin.android.extension.isExternalScheme
import one.mixin.android.extension.isExternalTransferUrl
import one.mixin.android.extension.isLightningUrl
import one.mixin.android.extension.isMixinUrl
import one.mixin.android.tip.wc.WalletConnect
import one.mixin.android.ui.common.BaseFragment
import one.mixin.android.ui.home.MainActivity
import one.mixin.android.widget.PseudoNotificationView

abstract class VisionFragment : BaseFragment() {
    protected var pseudoNotificationView: PseudoNotificationView? = null

    protected val scanner: BarcodeScanner =
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(FORMAT_QR_CODE)
                .build(),
        )

    protected var fromShortcut = false

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        pseudoNotificationView =
            view.findViewById<PseudoNotificationView>(R.id.pseudo_view)?.apply {
                translationY = -300.dp.toFloat()
                callback = pseudoViewCallback
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scanner.closeSilently()
    }

    private val pseudoViewCallback =
        object : PseudoNotificationView.Callback {
            override fun onClick(content: String) {
                handleResult(requireActivity(), fromShortcut, content)
            }
        }
}

fun handleResult(
    activity: FragmentActivity,
    fromShortcut: Boolean,
    content: String,
) {
    val result =
        if (fromShortcut) {
            Intent(activity, MainActivity::class.java)
        } else {
            Intent()
        }
    if (content.isExternalScheme(activity) || content.isExternalTransferUrl() || content.isLightningUrl()) {
        result.putExtra(MainActivity.URL, content)
    } else if (content.startsWith(Constants.Scheme.WALLET_CONNECT_PREFIX) && WalletConnect.isEnabled()) {
        result.putExtra(MainActivity.WALLET_CONNECT, content)
    } else if (!content.isMixinUrl()) {
        result.putExtra(MainActivity.SCAN, content)
    } else if (content.startsWith(Constants.Scheme.TRANSFER, true) ||
        content.startsWith(Constants.Scheme.HTTPS_TRANSFER, true)
    ) {
        val segments = Uri.parse(content).pathSegments
        if (segments.isEmpty()) return

        val userId =
            if (segments.size >= 2) {
                segments[1]
            } else {
                segments[0]
            }
        result.putExtra(MainActivity.TRANSFER, userId)
    } else {
        result.putExtra(MainActivity.URL, content)
    }
    if (fromShortcut) {
        MainActivity.showFromShortcut(activity, result)
    } else {
        activity.setResult(Activity.RESULT_OK, result)
        activity.finish()
    }
}
