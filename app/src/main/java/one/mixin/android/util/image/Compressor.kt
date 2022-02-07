package one.mixin.android.util.image

import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.IOException

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

    @Throws(IOException::class)
    fun compressToFile(imageUri: Uri, compressedFilePath: String): File {
        return ImageUtil.compressImage(
            imageUri,
            maxWidth,
            maxHeight,
            compressFormat,
            quality,
            compressedFilePath
        )
    }
}
