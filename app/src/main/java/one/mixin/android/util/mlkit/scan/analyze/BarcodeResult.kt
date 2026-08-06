package one.mixin.android.util.mlkit.scan.analyze

import android.graphics.Point
import android.graphics.Rect
import com.google.mlkit.vision.barcode.common.Barcode

data class BarcodeScanItem(
    val text: String,
    val boundingBox: Rect?,
    val cornerPoints: List<Point>,
    val sourceWidth: Int,
    val sourceHeight: Int,
)

class BarcodeResult(
    val items: List<BarcodeScanItem>,
    val content: String? = null,
    val barcodes: List<Barcode>? = null,
) {
    constructor(barcodes: List<Barcode>?, content: String?) : this(
        items = emptyList(),
        content = content,
        barcodes = barcodes,
    )
}
