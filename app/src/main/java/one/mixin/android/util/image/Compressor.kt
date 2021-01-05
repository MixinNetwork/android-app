package one.mixin.android.util.image

import android.graphics.Bitmap
import java.io.File

class Compressor {
    private var maxWidth = 1920
    private var maxHeight = 1920
    private var compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    private var quality = 85

    fun setMaxWidth(maxWidth: Int): Compressor {
        this.maxWidth = maxWidth
        return this
    }

    fun setMaxHeight(maxHeight: Int): Compressor {
        this.maxHeight = maxHeight
        return this
    }

    fun setCompressFormat(compressFormat: Bitmap.CompressFormat): Compressor {
        this.compressFormat = compressFormat
        return this
    }

    fun setQuality(quality: Int): Compressor {
        this.quality = quality
        return this
    }

    @JvmOverloads
    fun compressToFile(imageFile: File, compressedFilePath: String = imageFile.name): File {
        return ImageUtil.compressImage(
            imageFile,
            maxWidth,
            maxHeight,
            compressFormat,
            quality,
            compressedFilePath
        )
    }

    fun compressToBitmap(imageFile: File): Bitmap {
        return ImageUtil.decodeSampledBitmapFromFile(imageFile, maxWidth, maxHeight)
    }
}
