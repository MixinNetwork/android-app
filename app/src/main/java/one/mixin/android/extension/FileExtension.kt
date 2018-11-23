package one.mixin.android.extension

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.ImageColumns
import android.util.Base64
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.EnvironmentCompat
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.NonCancellable.children
import one.mixin.android.MixinApplication
import one.mixin.android.widget.gallery.MimeType
import org.spongycastle.asn1.cmc.CMCStatus.success
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Stack

private fun isAvailable(): Boolean {
    val state = Environment.getExternalStorageState()
    if (Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state) {
        return true
    }
    return false
}

private fun hasWritePermission(): Boolean {
    return ContextCompat.checkSelfPermission(MixinApplication.appContext,
        Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

private fun Context.getAppPath(): File {
    return if (!hasWritePermission()) {
        getBestAvailableCacheRoot()
    } else if (isAvailable()) {
        File(
            "${Environment.getExternalStorageDirectory()}${File.separator}Mixin${File.separator}"
        )
    } else {
        var externalFile: Array<File>? = ContextCompat.getExternalFilesDirs(this, null)
        if (externalFile == null) {
            externalFile = arrayOf(this.getExternalFilesDir(null))
        }
        val root = File("${externalFile[0]}${File.separator}Mixin${File.separator}")
        root.mkdirs()
        return if (root.exists()) {
            root
        } else {
            getBestAvailableCacheRoot()
        }
    }
}

fun Context.getMediaPath(): File {
    return File("${getAppPath().absolutePath}${File.separator}Media${File.separator}")
}

fun getMimeType(uri: Uri): String? {
    var type: String? = null
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        type = MixinApplication.get().contentResolver.getType(uri)
    } else {
        val extension = try {
            MimeTypeMap.getFileExtensionFromUrl(uri.getFilePath())
        } catch (e: Exception) {
            null
        }
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
    }
    return type
}

fun String.isImageSupport(): Boolean {
    return this.equals(MimeType.GIF.toString(), true) ||
        this.equals(MimeType.JPEG.toString(), true) ||
        this.equals(MimeType.PNG.toString(), true)
}

fun String.isStickerSupport(): Boolean {
    return this.equals(MimeType.GIF.toString(), true) ||
        this.equals(MimeType.JPEG.toString(), true) ||
        this.equals(MimeType.WEBP.toString(), true) ||
        this.equals(MimeType.PNG.toString(), true)
}

fun getImageSize(file: File): Size {
    val options = BitmapFactory.Options()
    options.inJustDecodeBounds = true
    BitmapFactory.decodeFile(file.absolutePath, options)
    val height = options.outHeight
    val width = options.outWidth
    when (getOrientationFromExif(file.absolutePath)) {
        90, 270 -> {
            return Size(height, width)
        }
    }
    return Size(width, height)
}

fun String.fileExists(): Boolean {
    return File(this.toUri().getFilePath(MixinApplication.appContext)).exists()
}

private fun getOrientationFromExif(imagePath: String): Int {
    var orientation = -1
    val exif = ExifInterface(imagePath)
    val exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL)
    when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_270 -> orientation = 270
        ExifInterface.ORIENTATION_ROTATE_180 -> orientation = 180
        ExifInterface.ORIENTATION_ROTATE_90 -> orientation = 90
        ExifInterface.ORIENTATION_NORMAL -> orientation = 0
    }
    return orientation
}

private fun Context.getBestAvailableCacheRoot(): File {
    val roots = ContextCompat.getExternalCacheDirs(this)
    roots.filter { it != null && Environment.MEDIA_MOUNTED == EnvironmentCompat.getStorageState(it) }
        .forEach { return it }
    return this.cacheDir
}

fun Context.getImagePath(): File {
    val root = getMediaPath()
    return File("$root${File.separator}Images")
}

fun Context.getDocumentPath(): File {
    val root = getMediaPath()
    return File("$root${File.separator}Files")
}

fun Context.getVideoPath(): File {
    val root = getMediaPath()
    return File("$root${File.separator}Video")
}

fun Context.getAudioPath(): File {
    val root = getMediaPath()
    return File("$root${File.separator}Audio")
}

fun Context.getPublicPictyresPath(): File {
    return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Mixin")
}

fun Context.getImageCachePath(): File {
    val root = getBestAvailableCacheRoot()
    return File("$root${File.separator}Images")
}

fun Context.isQRCodeFileExists(name: String): Boolean {
    val root = getBestAvailableCacheRoot()
    val file = File("$root${File.separator}$name.png")
    return file.exists() && file.length() > 0
}

fun Context.getQRCodePath(name: String): File {
    val root = getBestAvailableCacheRoot()
    val file = File("$root${File.separator}$name.png")
    if (!file.exists()) {
        file.createNewFile()
    }
    return file
}

fun Context.getGroupAvatarPath(name: String, create: Boolean = true): File {
    val root = getBestAvailableCacheRoot()
    val file = File("$root${File.separator}$name.png")
    if (create && !file.exists()) {
        file.createNewFile()
    }
    return file
}

fun File.createNoMediaDir() {
    val no = File(this, ".nomedia")
    if (!no.exists()) {
        no.createNewFile()
    }
}

fun File.createImageTemp(prefix: String? = null, type: String? = null, noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return if (prefix != null) {
        newTempFile("${prefix}_IMAGE_$time", type ?: ".jpg", noMedia)
    } else {
        newTempFile("IMAGE_$time", type ?: ".jpg", noMedia)
    }
}

fun File.createGifTemp(noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return newTempFile("IMAGE_$time", ".gif", noMedia)
}

fun File.createWebpTemp(noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return newTempFile("IMAGE_$time", ".webp", noMedia)
}

fun File.createDocumentTemp(type: String?, noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return newTempFile("FILE_$time", if (type == null) {
        ""
    } else {
        ".$type"
    }, noMedia)
}

fun File.createVideoTemp(type: String, noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return newTempFile("VIDEO_$time", ".$type", noMedia)
}

fun File.createAudioTemp(type: String, noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    return newTempFile("Audio_$time", ".$type", noMedia)
}

private fun File.newTempFile(name: String, type: String, noMedia: Boolean): File {
    if (!this.exists()) {
        this.mkdirs()
    }
    if (noMedia) {
        createNoMediaDir()
    }
    return createTempFile(name, type, this)
}

fun File.processing(to: File) {
    val inStream = FileInputStream(this)
    val outStream = FileOutputStream(to)
    val inChannel = inStream.channel
    inChannel.transferTo(0, inChannel.size(), outStream.channel)
    inStream.close()
    outStream.close()
}

fun String.getFilePath(): String? = Uri.parse(this).getFilePath()

fun Uri.getFilePath(context: Context = MixinApplication.appContext): String? {
    val scheme = this.scheme
    var data: String? = null
    if (scheme == null)
        data = this.toString()
    else if (ContentResolver.SCHEME_FILE == scheme) {
        data = this.path
    } else if (ContentResolver.SCHEME_CONTENT == scheme) {
        val cursor = context.contentResolver.query(this, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
        if (null != cursor) {
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(ImageColumns.DATA)
                if (index > -1) {
                    data = cursor.getString(index)
                    if (data == null) {
                        return getImageUrlWithAuthority(context)
                    }
                } else if (index == -1) {
                    return getImageUrlWithAuthority(context)
                }
            }
            cursor.close()
        } else {
            return getImageUrlWithAuthority(context)
        }
    }
    return data
}

fun Uri.getImageUrlWithAuthority(context: Context): String? {
    if (this.authority != null) {
        var input: InputStream? = null
        try {
            input = context.contentResolver.openInputStream(this)
            val mimeTypeMap = MimeTypeMap.getSingleton()
            val type = mimeTypeMap.getExtensionFromMimeType(context.contentResolver.getType(this))
            val outFile = context.getImageCachePath().createImageTemp(type = ".$type")
            outFile.copyFromInputStream(input)
            return outFile.absolutePath
        } catch (ignored: Exception) {
        } finally {
            input?.closeSilently()
        }
    }
    return null
}

fun File.copyFromInputStream(inputStream: InputStream) {
    inputStream.use { input ->
        this.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

fun File.copy(destFile: File) {
    if (!destFile.exists()) {
        destFile.createNewFile()
    }
    val src = FileInputStream(this).channel
    val dest = FileOutputStream(destFile).channel
    dest.transferFrom(src, 0, src.size())
    src.closeSilently()
    dest.closeSilently()
}

fun File.blurThumbnail(size: Size): Bitmap? {
    var scale = 1
    do {
        if (maxOf(size.width, size.height) / scale > 64) {
            scale++
        } else {
            break
        }
    } while (true)
    return blurThumbnail(size.width / scale, size.height / scale)
}

fun File.dirSize(): Long? {
    return if (isDirectory) {
        var result = 0L
        val dirList = Stack<File>()
        dirList.clear()
        dirList.push(this)
        while (!dirList.isEmpty()) {
            val dirCurrent = dirList.pop()
            val fileList = dirCurrent.listFiles()
            for (f in fileList) {
                if (f.isDirectory) {
                    dirList.push(f)
                } else {
                    result += f.length()
                }
            }
        }
        return result
    } else {
        null
    }
}

fun File.moveChileFileToDir(dir: File) {
    if (!dir.exists()) {
        dir.mkdirs()
    }
    if (isDirectory && dir.isDirectory) {
        for (chile in listFiles()) {
            if (chile.length() > 0 && chile.isFile) {
                chile.renameTo(File("${dir.absolutePath}${File.separator}${chile.name}"))
            }
        }
    }
}

fun File.deleteDir() {
    if (isDirectory()) {
        val children = listFiles()
        for (child in children) {
            child.deleteDir()
        }
    }
    delete()
}

fun Bitmap.rotate(angle: String): Bitmap? {
    val matrix = Matrix()
    when (angle) {
        "90" -> matrix.postRotate(90f)
        "180" -> matrix.postRotate(180f)
        "270" -> matrix.postRotate(270f)
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Bitmap.zoomOut(): Bitmap? {
    var scale = 1
    do {
        if (maxOf(width, height) / scale > 64) {
            scale++
        } else {
            break
        }
    } while (true)
    return Bitmap.createScaledBitmap(this, (width / scale), (height / scale), false)
}

private fun File.blurThumbnail(width: Int, height: Int): Bitmap? {
    try {
        return ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(this.absolutePath),
            width, height).fastBlur(1f, 10)
    } catch (e: Exception) {
    }
    return null
}

fun String.decodeBase64(): ByteArray {
    return Base64.decode(this, 0)
}

fun Bitmap.bitmap2String(mimeType: String = "", bitmapQuality: Int = 90): String? {
    val stream = ByteArrayOutputStream()
    if (mimeType == MimeType.PNG.toString()) {
        this.compress(Bitmap.CompressFormat.PNG, bitmapQuality, stream)
    } else {
        this.compress(Bitmap.CompressFormat.JPEG, bitmapQuality, stream)
    }
    val data = stream.toByteArray()
    stream.closeSilently()
    return Base64.encodeToString(data, Base64.NO_WRAP)
}

fun ByteArray.encodeBitmap(): Bitmap? {
    return if (this.isEmpty()) {
        null
    } else {
        BitmapFactory.decodeByteArray(this, 0, this.size)
    }
}

fun Bitmap.toDrawable(): Drawable = BitmapDrawable(MixinApplication.appContext.resources, this)

fun String.toDrawable() = this.decodeBase64().encodeBitmap()?.toDrawable()

fun String.getFileNameNoEx(): String {
    val dot = this.lastIndexOf('.')
    if (dot > -1 && dot < this.length) {
        return this.substring(0, dot)
    }
    return this
}

fun String.getExtensionName(): String? {
    if (this.isNotEmpty()) {
        val dot = this.lastIndexOf('.')
        if (dot > -1 && dot < this.length - 1) {
            return this.substring(dot + 1)
        }
    }
    return null
}

fun Bitmap.fastBlur(scale: Float, radius: Int): Bitmap? {
    var sentBitmap = this

    val width = Math.round(sentBitmap.width * scale)
    val height = Math.round(sentBitmap.height * scale)
    sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)

    val bitmap = sentBitmap.copy(sentBitmap.config ?: Bitmap.Config.ARGB_8888, true)

    if (radius < 1) {
        return null
    }

    val w = bitmap.width
    val h = bitmap.height

    val pix = IntArray(w * h)
    bitmap.getPixels(pix, 0, w, 0, 0, w, h)

    val wm = w - 1
    val hm = h - 1
    val wh = w * h
    val div = radius + radius + 1

    val r = IntArray(wh)
    val g = IntArray(wh)
    val b = IntArray(wh)
    var rsum: Int
    var gsum: Int
    var bsum: Int
    var x: Int
    var y: Int
    var i: Int
    var p: Int
    var yp: Int
    var yi: Int
    var yw: Int
    val vmin = IntArray(Math.max(w, h))

    var divsum = div + 1 shr 1
    divsum *= divsum
    val dv = IntArray(256 * divsum)
    i = 0
    while (i < 256 * divsum) {
        dv[i] = i / divsum
        i++
    }

    yi = 0
    yw = yi

    val stack = Array(div) { IntArray(3) }
    var stackpointer: Int
    var stackstart: Int
    var sir: IntArray
    var rbs: Int
    val r1 = radius + 1
    var routsum: Int
    var goutsum: Int
    var boutsum: Int
    var rinsum: Int
    var ginsum: Int
    var binsum: Int

    y = 0
    while (y < h) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        i = -radius
        while (i <= radius) {
            p = pix[yi + Math.min(wm, Math.max(i, 0))]
            sir = stack[i + radius]
            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff
            rbs = r1 - Math.abs(i)
            rsum += sir[0] * rbs
            gsum += sir[1] * rbs
            bsum += sir[2] * rbs
            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }
            i++
        }
        stackpointer = radius

        x = 0
        while (x < w) {

            r[yi] = dv[rsum]
            g[yi] = dv[gsum]
            b[yi] = dv[bsum]

            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum

            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]

            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]

            if (y == 0) {
                vmin[x] = Math.min(x + radius + 1, wm)
            }
            p = pix[yw + vmin[x]]

            sir[0] = p and 0xff0000 shr 16
            sir[1] = p and 0x00ff00 shr 8
            sir[2] = p and 0x0000ff

            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]

            rsum += rinsum
            gsum += ginsum
            bsum += binsum

            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer % div]

            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]

            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]

            yi++
            x++
        }
        yw += w
        y++
    }
    x = 0
    while (x < w) {
        bsum = 0
        gsum = bsum
        rsum = gsum
        boutsum = rsum
        goutsum = boutsum
        routsum = goutsum
        binsum = routsum
        ginsum = binsum
        rinsum = ginsum
        yp = -radius * w
        i = -radius
        while (i <= radius) {
            yi = Math.max(0, yp) + x

            sir = stack[i + radius]

            sir[0] = r[yi]
            sir[1] = g[yi]
            sir[2] = b[yi]

            rbs = r1 - Math.abs(i)

            rsum += r[yi] * rbs
            gsum += g[yi] * rbs
            bsum += b[yi] * rbs

            if (i > 0) {
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
            } else {
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
            }

            if (i < hm) {
                yp += w
            }
            i++
        }
        yi = x
        stackpointer = radius
        y = 0
        while (y < h) {
            // Preserve alpha channel: ( 0xff000000 & pix[yi] )
            pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

            rsum -= routsum
            gsum -= goutsum
            bsum -= boutsum

            stackstart = stackpointer - radius + div
            sir = stack[stackstart % div]

            routsum -= sir[0]
            goutsum -= sir[1]
            boutsum -= sir[2]

            if (x == 0) {
                vmin[y] = Math.min(y + r1, hm) * w
            }
            p = x + vmin[y]

            sir[0] = r[p]
            sir[1] = g[p]
            sir[2] = b[p]

            rinsum += sir[0]
            ginsum += sir[1]
            binsum += sir[2]

            rsum += rinsum
            gsum += ginsum
            bsum += binsum

            stackpointer = (stackpointer + 1) % div
            sir = stack[stackpointer]

            routsum += sir[0]
            goutsum += sir[1]
            boutsum += sir[2]

            rinsum -= sir[0]
            ginsum -= sir[1]
            binsum -= sir[2]

            yi += w
            y++
        }
        x++
    }

    bitmap.setPixels(pix, 0, w, 0, 0, w, h)

    return bitmap
}

fun File.toByteArray(): ByteArray? {
    var byteArray: ByteArray? = null
    try {
        val inputStream = FileInputStream(this)
        val bos = ByteArrayOutputStream()
        val b = ByteArray(1024 * 8)

        while (inputStream.read(b) != -1) {
            bos.write(b, 0, b.size)
        }

        byteArray = bos.toByteArray()
    } catch (e: IOException) {
        e.printStackTrace()
    }

    return byteArray
}