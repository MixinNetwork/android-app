package one.mixin.android.extension

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.FormatException
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.EnumMap

fun Bitmap.toBytes(): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 100, stream)
    val data = stream.toByteArray()
    stream.closeSilently()
    return data
}

fun Bitmap.saveQRCode(ctx: Context, name: String? = null) {
    val bos = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, bos)
    val fos = if (name != null) {
        FileOutputStream(ctx.getAddressCodePath(name))
    } else {
        FileOutputStream(ctx.getQRCodePath())
    }
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

fun Bitmap.decodeQR(): String? {
    val width = width
    val height = height
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)

    val source = RGBLuminanceSource(width, height, pixels)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

    val reader = QRCodeReader()
    var result: Result? = null
    try {
        val tmpHintsMap = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        tmpHintsMap[DecodeHintType.TRY_HARDER] = true
        result = reader.decode(binaryBitmap, tmpHintsMap)
    } catch (e: NotFoundException) {
        e.printStackTrace()
    } catch (e: ChecksumException) {
        e.printStackTrace()
    } catch (e: FormatException) {
        e.printStackTrace()
    }
    return result?.text
}