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

class Compressor {
    private var maxWidth = 1920
    private var maxHeight = 1920
    private var compressFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    private var quality = 85

    fun compressToFile(imageFile: File): File {
        val options = BitmapFactory.Options()
        var scaledBitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)
        options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
        options.inJustDecodeBounds = false

        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
        val matrix = Matrix()
        when (orientation) {
            ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrix, true)
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(imageFile)
            scaledBitmap.compress(compressFormat, quality, fileOutputStream);
        } finally {
            if (fileOutputStream != null) {
                fileOutputStream.flush()
                fileOutputStream.close()
            }
        }
        return imageFile
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height / width >= 3 || width / height >= 3) {
            return inSampleSize
        }

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
