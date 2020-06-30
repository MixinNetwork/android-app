package one.mixin.android.ui.qr

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.extension.closeSilently
import one.mixin.android.extension.decodeQR

class QRCodeProcessor {
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    fun detect(
        coroutineScope: CoroutineScope,
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception?) -> Unit,
        onComplete: (() -> Unit)? = null
    ) = coroutineScope.launch {
        try {
            var url: String? = null
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    url = barcodes.firstOrNull()?.rawValue
                    url?.let { onSuccess(it) }
                }
                .addOnFailureListener {
                    onFailure(it)
                }
                .addOnCompleteListener {
                    if (url == null) {
                        decodeWithZxing(coroutineScope, bitmap, onSuccess, onFailure, onComplete)
                    } else {
                        onComplete?.invoke()
                    }
                }
        } catch (e: Exception) {
            decodeWithZxing(coroutineScope, bitmap, onSuccess, onFailure, onComplete)
        }
    }

    private fun decodeWithZxing(
        coroutineScope: CoroutineScope,
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception?) -> Unit,
        onComplete: (() -> Unit)? = null
    ) = coroutineScope.launch {
        val url = withContext(Dispatchers.IO) {
            bitmap.decodeQR()
        }
        onComplete?.invoke()
        if (url != null) {
            onSuccess(url)
        } else {
            onFailure(null)
        }
    }

    fun close() {
        scanner.closeSilently()
    }
}
