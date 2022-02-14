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
        destinationPath: String
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
    fun decodeSampledBitmapFromFile(imageUri: Uri, reqWidth: Int, reqHeight: Int): Bitmap {
        val imageInputStream = MixinApplication.get().contentResolver.openInputStream(imageUri)!!
        val options = BitmapFactory.Options()
        var bitmap = requireNotNull(BitmapFactory.decodeStream(imageInputStream, null, options))
        val scale = calculateInScale(bitmap.width, bitmap.height, reqWidth, reqHeight)
        val exif = ExifInterface(MixinApplication.get().contentResolver.openInputStream(imageUri)!!)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        }
        matrix.postScale(scale, scale)
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        return bitmap
    }

    private fun calculateInScale(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Float {

        if (width == 0 || height == 0 || height / width >= 3 || width / height >= 3) {
            return 1f
        }
        return if (width > height) {
            reqWidth / width.toFloat()
        } else {
            reqHeight / height.toFloat()
        }
    }
}
