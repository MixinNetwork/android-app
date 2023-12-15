package one.mixin.android.util.mlkit.scan.analyze

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import one.mixin.android.extension.decodeQR
import one.mixin.android.util.mlkit.scan.analyze.Analyzer.OnAnalyzeListener
import one.mixin.android.util.mlkit.scan.utils.BitmapUtils
import one.mixin.android.util.reportException

class BarcodeScanningAnalyzer : Analyzer<BarcodeResult> {
    private var mDetector: BarcodeScanner? = null

    constructor(
        @Barcode.BarcodeFormat barcodeFormat: Int,
        @Barcode.BarcodeFormat vararg barcodeFormats: Int,
    ) : this(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(barcodeFormat, *barcodeFormats)
            .build(),
    )

    constructor(options: BarcodeScannerOptions?) {
        mDetector = try {
            if (options != null) {
                BarcodeScanning.getClient(options)
            } else {
                BarcodeScanning.getClient()
            }
        } catch (e: Exception) {
            reportException(e)
            null
        }
    }

    override fun analyze(
        imageProxy: ImageProxy,
        listener: OnAnalyzeListener<AnalyzeResult<BarcodeResult>>,
    ) {
        try {
            val bitmap = BitmapUtils.getBitmap(imageProxy)
            val inputImage = InputImage.fromBitmap(bitmap!!, 0)
            mDetector?.process(inputImage)
                ?.addOnSuccessListener { result: List<Barcode>? ->
                    if (result.isNullOrEmpty()) {
                        listener.onFailure()
                    } else {
                        listener.onSuccess(AnalyzeResult(bitmap, BarcodeResult(result, null)))
                    }
                }?.addOnFailureListener { e: Exception? -> listener.onFailure() }
        } catch (e: Exception) {
            val bitmap = BitmapUtils.getBitmap(imageProxy)
            val result = bitmap?.decodeQR()
            if (result != null) {
                listener.onSuccess(AnalyzeResult(bitmap, BarcodeResult(null, result)))
            } else {
                listener.onFailure()
            }
            reportException(e) // Report mlkit exception
        }
    }
}
