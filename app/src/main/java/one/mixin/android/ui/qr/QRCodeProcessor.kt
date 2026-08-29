package one.mixin.android.ui.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import one.mixin.android.extension.closeSilently
import one.mixin.android.extension.decodeQR
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class QRCodeProcessor {
    private val scanner: BarcodeScanner =
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(FORMAT_QR_CODE)
                .build(),
        )

    fun detect(
        coroutineScope: CoroutineScope,
        bitmap: Bitmap,
        onSuccess: (String) -> Unit,
        onFailure: (Exception?) -> Unit,
        onComplete: (() -> Unit)? = null,
    ) = detectAll(
        coroutineScope,
        bitmap,
        onSuccess = { results -> onSuccess(results.first()) },
        onFailure = onFailure,
        onComplete = onComplete,
    )

    fun detectAll(
        coroutineScope: CoroutineScope,
        bitmap: Bitmap,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception?) -> Unit,
        onComplete: (() -> Unit)? = null,
    ) = coroutineScope.launch {
        var failure: Exception? = null
        val createdBitmaps = mutableListOf<Bitmap>()
        val sourceBitmap =
            if (bitmap.config == Bitmap.Config.HARDWARE) {
                bitmap.copy(Bitmap.Config.ARGB_8888, false).also { createdBitmaps.add(it) }
            } else {
                bitmap
            }
        try {
            val candidates =
                withContext(Dispatchers.IO) {
                    val inverted = invert(sourceBitmap).also { createdBitmaps.add(it) }
                    val monochrome = monochrome(inverted, 90).also { createdBitmaps.add(it) }
                    listOf(sourceBitmap, inverted, monochrome)
                }
            for (candidate in candidates) {
                try {
                    val results = detectWithMlKit(candidate)
                    if (results.isNotEmpty()) {
                        onSuccess(results)
                        onComplete?.invoke()
                        return@launch
                    }
                } catch (e: Exception) {
                    failure = e
                }
            }
            val url =
                withContext(Dispatchers.IO) {
                    sourceBitmap.decodeQR()
                }
            if (url != null) {
                onSuccess(listOf(url))
                onComplete?.invoke()
            } else {
                onComplete?.invoke()
                onFailure(failure)
            }
        } catch (e: Exception) {
            val url =
                withContext(Dispatchers.IO) {
                    sourceBitmap.decodeQR()
                }
            if (url != null) {
                onSuccess(listOf(url))
                onComplete?.invoke()
            } else {
                onComplete?.invoke()
                onFailure(e)
            }
        } finally {
            createdBitmaps.forEach { candidate ->
                if (candidate !== bitmap && !candidate.isRecycled) {
                    candidate.recycle()
                }
            }
        }
    }

    private suspend fun detectWithMlKit(bitmap: Bitmap): List<String> =
        suspendCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    continuation.resume(
                        barcodes.mapNotNull { barcode ->
                            (barcode.rawValue ?: barcode.displayValue)?.takeIf { it.isNotBlank() }
                        },
                    )
                }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }

    private fun invert(bitmap: Bitmap): Bitmap {
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val paint = Paint()
        val matrixGrayscale = ColorMatrix().apply { setSaturation(0f) }
        val matrixInvert =
            ColorMatrix(
                floatArrayOf(
                    -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                    0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                    0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                    0.0f, 0.0f, 0.0f, 1.0f, 0.0f,
                ),
            )
        matrixInvert.preConcat(matrixGrayscale)
        paint.colorFilter = ColorMatrixColorFilter(matrixInvert)
        Canvas(newBitmap).drawBitmap(bitmap, 0f, 0f, paint)
        return newBitmap
    }

    private fun monochrome(
        bitmap: Bitmap,
        threshold: Int,
    ): Bitmap {
        val newBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val paint =
            Paint().apply {
                colorFilter = ColorMatrixColorFilter(createThresholdMatrix(threshold))
            }
        Canvas(newBitmap).drawBitmap(bitmap, 0f, 0f, paint)
        return newBitmap
    }

    private fun createThresholdMatrix(threshold: Int) =
        ColorMatrix(
            floatArrayOf(
                85f, 85f, 85f, 0f, -255f * threshold,
                85f, 85f, 85f, 0f, -255f * threshold,
                85f, 85f, 85f, 0f, -255f * threshold,
                0f, 0f, 0f, 1f, 0f,
            ),
        )

    fun close() {
        scanner.closeSilently()
    }
}
