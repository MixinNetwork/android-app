package one.mixin.android.util.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
import one.mixin.android.MixinApplication
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal object ImageUtil {
    @Throws(IOException::class)
    fun compressImage(
        imageUri: Uri,
        reqWidth: Int,
        reqHeight: Int,
        compressFormat: Bitmap.CompressFormat,
        quality: Int,
        destinationPath: String,
    ): File {
        var fileOutputStream: FileOutputStream? = null
        val file = File(destinationPath).parentFile
        if (file != null && !file.exists()) {
            file.mkdirs()
        }
        try {
            fileOutputStream = FileOutputStream(destinationPath)
            decodeSampledBitmapFromFile(imageUri, reqWidth, reqHeight)
                .compress(compressFormat, quality, fileOutputStream)
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.flush()
                fileOutputStream.close()
            }
        }

        return File(destinationPath)
    }

    @Throws(IOException::class)
    fun decodeSampledBitmapFromFile(
        imageUri: Uri,
        reqWidth: Int,
        reqHeight: Int,
    ): Bitmap {
        val resolver = MixinApplication.get().contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(imageUri) ?: throw IOException("Unable to open image")
        boundsStream.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Invalid image dimensions")
        }
        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > MAX_DECODED_DIMENSION ||
            bounds.outHeight / sampleSize > MAX_DECODED_DIMENSION ||
            bounds.outWidth.toLong() / sampleSize * (bounds.outHeight / sampleSize) > MAX_DECODED_PIXELS
        ) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        var bitmap =
            resolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: throw IOException("Unable to decode image")
        val scale = calculateInScale(bitmap.width, bitmap.height, reqWidth, reqHeight)
        val exif =
            resolver.openInputStream(imageUri)?.use {
                ExifInterface(it)
            } ?: throw IOException("Unable to read image metadata")
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        matrix.postScale(scale, scale)
        val source = bitmap
        bitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (bitmap !== source) source.recycle()
        return bitmap
    }

    private fun calculateInScale(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int,
    ): Float {
        if (width == 0 || height == 0 || height / width >= 3 || width / height >= 3) {
            return 1f
        }
        return if (width > height) {
            reqWidth / width.toFloat()
        } else {
            reqHeight / height.toFloat()
        }
    }

    private const val MAX_DECODED_DIMENSION = 8192
    private const val MAX_DECODED_PIXELS = 16_000_000L
}
