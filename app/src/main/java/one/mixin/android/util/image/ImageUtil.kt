package one.mixin.android.util.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270
import androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

internal object ImageUtil {

    @Throws(IOException::class)
    fun compressImage(
        imageFile: File,
        reqWidth: Int,
        reqHeight: Int,
        compressFormat:
            Bitmap.CompressFormat,
        quality: Int,
        destinationPath: String
    ): File {
        var fileOutputStream: FileOutputStream? = null
        val file = File(destinationPath).parentFile
        if (file != null && !file.exists()) {
            file.mkdirs()
        }
        try {
            fileOutputStream = FileOutputStream(destinationPath)
            decodeSampledBitmapFromFile(imageFile, reqWidth, reqHeight)
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
    fun decodeSampledBitmapFromFile(imageFile: File, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imageFile.absolutePath, options)

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

        options.inJustDecodeBounds = false

        var scaledBitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)

        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
        val matrix = Matrix()
        when (orientation) {
            ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            8 -> matrix.postRotate(ORIENTATION_ROTATE_270.toFloat())
        }
        scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrix, true)
        return scaledBitmap
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
