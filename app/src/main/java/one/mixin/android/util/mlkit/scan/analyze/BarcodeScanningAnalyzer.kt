package one.mixin.android.util.mlkit.scan.analyze

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import one.mixin.android.util.mlkit.scan.analyze.Analyzer.OnAnalyzeListener
import one.mixin.android.util.mlkit.scan.utils.BitmapUtils
import timber.log.Timber

class BarcodeScanningAnalyzer : Analyzer<List<Barcode>> {
    private var mDetector: BarcodeScanner? = null

    constructor() {
        mDetector = BarcodeScanning.getClient()
    }

    constructor(
        @Barcode.BarcodeFormat barcodeFormat: Int,
        @Barcode.BarcodeFormat vararg barcodeFormats: Int,
    ) : this(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(barcodeFormat, *barcodeFormats)
            .build(),
    ) {
    }

    constructor(options: BarcodeScannerOptions?) {
        mDetector = if (options != null) {
            BarcodeScanning.getClient(options)
        } else {
            BarcodeScanning.getClient()
        }
    }

    override fun analyze(
        imageProxy: ImageProxy,
        listener: OnAnalyzeListener<AnalyzeResult<List<Barcode>>>,
    ) {
        try {
            val bitmap = BitmapUtils.getBitmap(imageProxy)
            val inputImage = InputImage.fromBitmap(bitmap!!, 0)
            mDetector!!.process(inputImage)
                .addOnSuccessListener { result: List<Barcode>? ->
                    if (result.isNullOrEmpty()) {
                        listener.onFailure()
                    } else {
                        listener.onSuccess(AnalyzeResult(bitmap, result))
                    }
                }.addOnFailureListener { e: Exception? -> listener.onFailure() }
        } catch (e: Exception) {
            Timber.w(e)
        }
    }
}
