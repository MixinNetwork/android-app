package one.mixin.android.widget

import android.graphics.Bitmap
import coil.size.Size
import coil.transform.Transformation

class GrayscaleTransformation : Transformation {
    override val cacheKey: String
        get() = "grayscale_transformation"

    override suspend fun transform(
        input: Bitmap,
        size: Size,
    ): Bitmap {
        val width = input.width
        val height = input.height
        val grayBitmap = Bitmap.createBitmap(width, height, input.config)

        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = input.getPixel(x, y)
                val red = (pixel shr 16 and 0xff) * 0.299
                val green = (pixel shr 8 and 0xff) * 0.587
                val blue = (pixel and 0xff) * 0.114
                val gray = (red + green + blue).toInt()
                val newPixel = (0xff shl 24) or (gray shl 16) or (gray shl 8) or gray
                grayBitmap.setPixel(x, y, newPixel)
            }
        }
        return grayBitmap
    }
}
