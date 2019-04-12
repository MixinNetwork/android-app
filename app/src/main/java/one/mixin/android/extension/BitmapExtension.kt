package one.mixin.android.extension

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

fun Bitmap.toBytes(): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 100, stream)
    val data = stream.toByteArray()
    stream.closeSilently()
    return data
}

fun Bitmap.toPNGBytes(): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, stream)
    val data = stream.toByteArray()
    stream.closeSilently()
    return data
}

fun Bitmap.saveQRCode(ctx: Context, name: String) {
    val bos = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, bos)
    val fos = FileOutputStream(ctx.getQRCodePath(name))
    fos.write(bos.toByteArray())
    fos.flush()
    fos.closeSilently()
}

fun Bitmap.saveGroupAvatar(ctx: Context, name: String) {
    val bos = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, bos)
    val fos = FileOutputStream(ctx.getGroupAvatarPath(name))
    fos.write(bos.toByteArray())
    fos.flush()
    fos.closeSilently()
}

fun Bitmap.save(file: File) {
    val bos = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, bos)
    val fos = FileOutputStream(file)
    fos.write(bos.toByteArray())
    fos.flush()
    fos.closeSilently()
}

fun ByteArray.xYuv2Simple(w: Int, h: Int): ByteArray {
    val out = ByteArrayOutputStream()
    val yuvImage = YuvImage(this, ImageFormat.NV21, w, h, null)
    yuvImage.compressToJpeg(Rect(0, 0, w, h), 100, out)
    return out.toByteArray()
}

fun Bitmap.rotate(w: Int, h: Int, rotation: Int, isFacingBack: Boolean = false): Bitmap {
    val matrix = Matrix().apply {
        postRotate(rotation.toFloat())
        if (!isFacingBack) { // resolve mirror problem
            preScale(-1f, 1f)
        }
    }
    return Bitmap.createBitmap(this, 0, 0, w, h, matrix, true)
}

fun Bitmap.maxSizeScale(maxWidth: Int, maxHeight: Int): Bitmap {
    if (maxHeight > 0 && maxWidth > 0) {
        val width = this.width
        val height = this.height
        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight
        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }
        return Bitmap.createScaledBitmap(this, finalWidth, finalHeight, true)
    } else {
        return this
    }
}