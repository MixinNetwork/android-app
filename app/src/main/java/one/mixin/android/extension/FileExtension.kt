package one.mixin.android.extension

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Size
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.EnvironmentCompat
import androidx.exifinterface.media.ExifInterface
import one.mixin.android.Constants.Storage.AUDIO
import one.mixin.android.Constants.Storage.DATA
import one.mixin.android.Constants.Storage.IMAGE
import one.mixin.android.Constants.Storage.VIDEO
import one.mixin.android.MixinApplication
import one.mixin.android.session.Session
import one.mixin.android.util.blurhash.Base83
import one.mixin.android.util.blurhash.BlurHashDecoder
import one.mixin.android.util.blurhash.BlurHashEncoder
import one.mixin.android.util.reportException
import one.mixin.android.vo.StorageUsage
import one.mixin.android.widget.gallery.MimeType
import timber.log.Timber
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Scanner

private fun isAvailable(): Boolean {
    val state = Environment.getExternalStorageState()
    if (Environment.MEDIA_MOUNTED == state || Environment.MEDIA_MOUNTED_READ_ONLY == state) {
        return true
    }
    return false
}

fun hasWritePermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        MixinApplication.appContext,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.checkStorageNotLow(lowAction: () -> Unit, defaultAction: () -> Unit) {
    if (cacheDir.freeSpace < 100 * 1024 * 1024) { // 100MB
        lowAction()
    } else {
        defaultAction()
    }
}

private fun Context.getAppPath(legacy: Boolean = false): File? {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !legacy -> {
            externalMediaDirs.first()
        }
        hasWritePermission() && isAvailable() -> {
            File(
                "${Environment.getExternalStorageDirectory()}${File.separator}Mixin${File.separator}"
            )
        }
        else -> {
            null
        }
    }
}

fun Context.getMediaPath(legacy: Boolean = false): File? {
    val path = getAppPath(legacy) ?: return null
    val identityNumber = Session.getAccount()?.identityNumber ?: return null
    return File("${path.absolutePath}${File.separator}$identityNumber${File.separator}Media")
}

fun Context.getAncientMediaPath(): File? {
    val path = getAppPath() ?: return null
    val f = File("${path.absolutePath}${File.separator}Media")
    if (f.exists()) {
        return f
    }
    return null
}

fun Context.getLegacyBackupPath(create: Boolean = false, legacy: Boolean = false): File? {
    val path = getAppPath(legacy) ?: return null
    val identityNumber = Session.getAccount()?.identityNumber ?: return null
    val f = File("${path.absolutePath}${File.separator}$identityNumber${File.separator}Backup")
    if (create && (!f.exists() || !f.isDirectory)) {
        f.delete()
        f.mkdirs()
    }
    return f
}

fun Context.getOldBackupPath(create: Boolean = false, legacy: Boolean = false): File? {
    val path = getAppPath(legacy) ?: return null
    val identityNumber = Session.getAccount()?.identityNumber ?: return null
    val f = File("${path.absolutePath}${File.separator}Backup${File.separator}$identityNumber")
    if (create && (!f.exists() || !f.isDirectory)) {
        f.delete()
        f.mkdirs()
    }
    return f
}

fun Context.getCacheMediaPath(): File {
    return File("${getBestAvailableCacheRoot().absolutePath}${File.separator}Media${File.separator}")
}

fun getMimeType(
    uri: Uri,
    isImage: Boolean = false
): String? {
    var type: String? = null
    if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
        type = MixinApplication.get().contentResolver.getType(uri)
    } else {
        val path = uri.getFilePath()
        val extension = try {
            MimeTypeMap.getFileExtensionFromUrl(path)
        } catch (e: Exception) {
            null
        }
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
        if (isImage && type == null && path != null) {
            type = try {
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                FileInputStream(path).use {
                    BitmapFactory.decodeStream(it, null, options)
                }
                options.outMimeType
            } catch (e: Exception) {
                null
            }
        }
    }
    return type
}

fun String.isImageSupport(): Boolean {
    return this.equals(MimeType.GIF.toString(), true) ||
        this.equals(MimeType.JPEG.toString(), true) ||
        this.equals(MimeType.JPG.toString(), true) ||
        this.equals(MimeType.PNG.toString(), true) ||
        this.equals(MimeType.HEIC.toString(), true)
}

fun String.isStickerSupport(): Boolean {
    return this.equals(MimeType.GIF.toString(), true) ||
        this.equals(MimeType.JPEG.toString(), true) ||
        this.equals(MimeType.JPG.toString(), true) ||
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
    val path = this.toUri().getFilePath(MixinApplication.appContext) ?: return false
    return File(path).exists()
}

private fun getOrientationFromExif(imagePath: String): Int {
    var orientation = -1
    val exif = ExifInterface(imagePath)
    val exifOrientation = exif.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )
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

fun File.generateConversationPath(conversationId: String): File {
    return File("$this${File.separator}$conversationId")
}
fun Context.getImagePath(legacy: Boolean = false): File {
    val root = getMediaPath(legacy)
    return File("$root${File.separator}Images")
}

fun Context.getOtherPath(legacy: Boolean = false): File {
    val root = getMediaPath(legacy)
    return File("$root${File.separator}Others")
}

fun Context.getDocumentPath(legacy: Boolean = false): File {
    val root = getMediaPath(legacy)
    return File("$root${File.separator}Files")
}

fun Context.getVideoPath(legacy: Boolean = false): File {
    val root = getMediaPath(legacy)
    return File("$root${File.separator}Videos")
}

fun Context.getAudioPath(legacy: Boolean = false): File {
    val root = getMediaPath(legacy)
    return File("${root}${File.separator}Audios")
}

fun Context.getTranscriptFile(name: String, type: String, legacy: Boolean = false): File {
    return getTranscriptDirPath(legacy).newTempFile(
        name, type,
        true
    )
}

fun Context.getTranscriptDirPath(legacy: Boolean = false): File {
    val root = getMediaPath(legacy)
    return File("$root${File.separator}Transcripts${File.separator}")
}

fun Context.getConversationImagePath(conversationId: String): File? {
    if (conversationId.isBlank()) return null
    val root = getMediaPath() ?: return null
    return File("$root${File.separator}Images${File.separator}$conversationId")
}

fun Context.getConversationDocumentPath(conversationId: String): File? {
    if (conversationId.isBlank()) return null
    val root = getMediaPath() ?: return null
    return File("$root${File.separator}Files${File.separator}$conversationId")
}

fun Context.getConversationVideoPath(conversationId: String): File? {
    if (conversationId.isBlank()) return null
    val root = getMediaPath() ?: return null
    return File("$root${File.separator}Videos${File.separator}$conversationId")
}

fun Context.getConversationAudioPath(conversationId: String): File? {
    if (conversationId.isBlank()) return null
    val root = getMediaPath() ?: return null
    return File("$root${File.separator}Audios${File.separator}$conversationId")
}

fun Context.getConversationTranscriptPath(conversationId: String): File? {
    if (conversationId.isBlank()) return null
    val root = getMediaPath() ?: return null
    return File("$root${File.separator}Transcripts${File.separator}$conversationId")
}

fun Context.getConversationMediaSize(conversationId: String): Long {
    var mediaSize = 0L
    getConversationImagePath(conversationId)?.apply {
        if (exists()) {
            mediaSize += dirSize() ?: 0
        }
    }
    getConversationVideoPath(conversationId)?.apply {
        if (exists()) {
            mediaSize += dirSize() ?: 0
        }
    }
    getConversationAudioPath(conversationId)?.apply {
        if (exists()) {
            mediaSize += dirSize() ?: 0
        }
    }
    getConversationDocumentPath(conversationId)?.apply {
        if (exists()) {
            mediaSize += dirSize() ?: 0
        }
    }
    getConversationTranscriptPath(conversationId)?.apply {
        if (exists()) {
            mediaSize += dirSize() ?: 0
        }
    }
    return mediaSize
}

fun Context.getStorageUsageByConversationAndType(conversationId: String, type: String): StorageUsage? {
    val dir = when (type) {
        IMAGE -> getConversationImagePath(conversationId)
        VIDEO -> getConversationVideoPath(conversationId)
        AUDIO -> getConversationAudioPath(conversationId)
        DATA -> getConversationDocumentPath(conversationId)
        else -> null
    } ?: return null
    return dir.run {
        if (exists()) {
            val mediaSize = dirSize() ?: return@run null
            val count = list()?.size ?: return@run null
            if (mediaSize == 0L || count == 0) return@run null
            StorageUsage(conversationId, type, count, mediaSize)
        } else {
            null
        }
    }
}

fun Context.getPublicPicturePath(): File {
    return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Mixin")
}

fun Context.getPublicDocumentPath(): File {
    return File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Mixin")
}

fun Context.getImageCachePath(): File {
    val root = getBestAvailableCacheRoot()
    return File("$root${File.separator}Images")
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
    if (!exists()) {
        mkdirs()
    }
    if (!no.exists()) {
        no.createNewFile()
    }
}

fun File.createImageTemp(conversationId: String, messageId: String, type: String? = null, noMedia: Boolean = true): File {
    val conversationPath = generateConversationPath(conversationId)
    return conversationPath.newTempFile(messageId, type ?: ".jpg", noMedia)
}

fun File.createImageTemp(noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    return newTempFile("IMAGE_$time", ".jpg", noMedia)
}

fun File.createImagePngTemp(noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    return newTempFile("IMAGE_$time", ".png", noMedia)
}

fun File.createPostTemp(prefix: String? = null, type: String? = null): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    return if (prefix != null) {
        newTempFile("${prefix}_POST_$time", type ?: ".md", false)
    } else {
        newTempFile("POST_$time", type ?: ".md", false)
    }
}

fun File.createPdfTemp(prefix: String? = null): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
    return if (prefix != null) {
        newTempFile("${prefix}_POST_$time", ".pdf", false)
    } else {
        newTempFile("POST_$time", ".pdf", false)
    }
}

fun File.createGifTemp(conversationId: String, messageId: String, noMedia: Boolean = true): File {
    val path = generateConversationPath(conversationId)
    return path.newTempFile(messageId, ".gif", noMedia)
}

fun File.createGifTemp(noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    return newTempFile(time, ".gif", noMedia)
}

fun File.createPngTemp(noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    return newTempFile("IMAGE_$time", ".png", noMedia)
}

fun File.createWebpTemp(conversationId: String, messageId: String, noMedia: Boolean = true): File {
    val path = generateConversationPath(conversationId)
    return path.newTempFile(messageId, ".webp", noMedia)
}

fun File.createEmptyTemp(conversationId: String, messageId: String, noMedia: Boolean = true): File {
    val path = generateConversationPath(conversationId)
    return path.newTempFile(messageId, "", noMedia)
}

fun File.createDocumentTemp(conversationId: String, messageId: String, type: String?, noMedia: Boolean = true): File {
    val path = generateConversationPath(conversationId)
    return path.newTempFile(
        messageId,
        if (type == null) {
            ""
        } else {
            ".$type"
        },
        noMedia
    )
}

private fun File.createDocumentFile(
    noMedia: Boolean = true,
    name: String? = null,
    extensionName: String? = null
): Pair<File, Boolean> {
    val defaultName = "FILE_${
    SimpleDateFormat(
        "yyyyMMdd_HHmmss_SSS", Locale.US
    ).format(Date())
    }${
    if (extensionName == null) {
        ""
    } else {
        ".$extensionName"
    }
    }"
    val fileName = name ?: defaultName
    if (!this.exists()) {
        this.mkdirs()
    }
    if (noMedia) {
        createNoMediaDir()
    }
    val f = File(this, fileName)
    return Pair(f, f.exists())
}

fun File.createVideoTemp(conversationId: String, messageId: String, type: String, noMedia: Boolean = true): File {
    val path = generateConversationPath(conversationId)
    return path.newTempFile(messageId, ".$type", noMedia)
}

fun File.createVideoTemp(type: String, noMedia: Boolean = true): File {
    val time = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
    return newTempFile("VIDEO_$time", ".$type", noMedia)
}

fun File.createAudioTemp(conversationId: String, messageId: String, type: String, noMedia: Boolean = true): File {
    val path = generateConversationPath(conversationId)
    return path.newTempFile(messageId, ".$type", noMedia)
}

fun File.newTempFile(name: String, type: String, noMedia: Boolean): File {
    if (!this.exists()) {
        this.mkdirs()
    }
    if (noMedia) {
        createNoMediaDir()
    }
    return File(this, "$name$type")
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

@Deprecated("Don’t get the file path on the URI")
fun Uri.getFilePath(context: Context = MixinApplication.appContext): String? {
    val scheme = this.scheme
    var data: String? = null
    var reportException: Exception? = null
    when (scheme) {
        null -> data = this.toString()
        ContentResolver.SCHEME_FILE -> data = this.path
        ContentResolver.SCHEME_CONTENT -> {
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(this, arrayOf(MediaStore.Images.Media.DATA), null, null, null)
                if (null != cursor) {
                    if (cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
                        if (index > -1) {
                            data = cursor.getString(index)
                            if (data == null) {
                                data = copyFileUrlWithAuthority(context)
                            }
                        } else if (index == -1) {
                            data = copyFileUrlWithAuthority(context)
                        }
                    }
                } else {
                    data = copyFileUrlWithAuthority(context)
                }
            } catch (e: SecurityException) {
                Timber.w(e)
                reportException = e
            } catch (e: UnsupportedOperationException) {
                cursor = context.contentResolver.query(this, null, null, null, null)
                val name = try {
                    if (cursor != null && cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        cursor.getString(index)
                    } else null
                } catch (e: Exception) {
                    null
                }
                data = copyFileUrlWithAuthority(context, name)
            } finally {
                cursor?.close()
            }
        }
    }
    if (data == null) {
        if (reportException == null) {
            reportException = IllegalStateException("Uri.getFilePath data == null")
        }
        reportException("Uri.getFilePath uri: $this", reportException)
    }
    return data
}

fun Uri.getFileName(context: Context = MixinApplication.appContext): String {
    try {
        var result: String? = null
        if (scheme.equals(ContentResolver.SCHEME_CONTENT)) {
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                cursor?.let { c ->
                    if (c.moveToFirst()) {
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        result = c.getString(index)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e)
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result!!.substring(cut + 1)
            }
        }
        return result ?: ""
    } catch (e: java.lang.Exception) {
        Timber.e(e)
    }
    return ""
}

fun Uri.copyFileUrlWithAuthority(context: Context, name: String? = null): String? {
    if (this.authority != null) {
        var input: InputStream? = null
        return try {
            val extensionName = getFileName(context).getExtensionName()
            input = context.contentResolver.openInputStream(this) ?: return null
            val pair = context.getDocumentPath()?.createDocumentFile(name = name, extensionName = extensionName) ?: return null
            val outFile = pair.first
            if (!pair.second) {
                outFile.copyFromInputStream(input)
            }
            outFile.absolutePath
        } catch (e: Exception) {
            reportException("Uri.copyFileUrlWithAuthority name: $name", e)
            null
        } finally {
            input?.closeSilently()
        }
    }
    return null
}

fun Uri.getOrCreate(context: Context = MixinApplication.appContext, name: String): String? {
    val file = File(context.getDocumentPath(), name)
    if (!file.exists()) {
        context.contentResolver.openInputStream(this)?.let {
            file.copyFromInputStream(it)
        }
    }
    return file.absolutePath
}

fun File.copyFromInputStream(inputStream: InputStream) {
    inputStream.use { input ->
        this.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

fun File.deleteOnExists() {
    if (exists()) {
        delete()
    }
}

fun Uri.copyFromInputStream(inputStream: InputStream) {
    inputStream.use { input ->
        MixinApplication.appContext.contentResolver.openOutputStream(this, "w")?.use { output ->
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
        if (maxOf(size.width, size.height) / scale > 48) {
            scale++
        } else {
            break
        }
    } while (true)
    return blurThumbnail(size.width / scale, size.height / scale)
}

fun File.dirSize(): Long? {
    return if (isDirectory) {
        return getDirSize(this)
    } else {
        null
    }
}

private fun getDirSize(dir: File): Long? {
    try {
        val du = Runtime.getRuntime().exec(
            "/system/bin/du -s " + dir.canonicalPath,
            arrayOf(),
            Environment.getRootDirectory()
        )
        val br = BufferedReader(InputStreamReader(du.inputStream))
        return Scanner(br.readLine()).nextLong().apply {
            Timber.e("$dir: $this")
        }
    } catch (e: Exception) {
        Timber.e(e.message)
    }
    return null
}

fun File.encodeBlurHash(): String? {
    return BlurHashEncoder.encode(inputStream())
}

fun String.decodeBlurHash(width: Int, height: Int): Bitmap? {
    return BlurHashDecoder.decode(this, width, height, 1.0)
}

fun File.moveChileFileToDir(dir: File, eachCallback: ((newFile: File, oldFile: File) -> Unit)? = null) {
    if (!dir.exists()) {
        dir.mkdirs()
    }
    if (isDirectory && dir.isDirectory) {
        listFiles()?.forEach { child ->
            if (child.length() > 0 && child.isFile) {
                val newFile = File("${dir.absolutePath}${File.separator}${child.name}")
                child.renameTo(newFile)
                eachCallback?.invoke(newFile, child)
            }
        }
    }
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
        return ThumbnailUtils.extractThumbnail(
            BitmapFactory.decodeFile(this.absolutePath),
            width,
            height
        ).fastBlur(1f, 10)
    } catch (e: Exception) {
    }
    return null
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

private const val MAX_BLUR_HASH_DIMEN = 20

fun String.toDrawable(width: Int, height: Int): Drawable? {
    return try {
        if (!Base83.isValid(this)) {
            this.decodeBase64().encodeBitmap()?.toDrawable()
        } else {
            BlurHashDecoder.decode(this, minOf(width, MAX_BLUR_HASH_DIMEN), minOf(height, MAX_BLUR_HASH_DIMEN), 1.0)?.toDrawable()
        }
    } catch (e: Exception) {
        null
    }
}

fun String.toBitmap(width: Int, height: Int): Bitmap? {
    return try {
        if (!Base83.isValid(this)) {
            this.decodeBase64().encodeBitmap()
        } else {
            BlurHashDecoder.decode(this, minOf(width, MAX_BLUR_HASH_DIMEN), minOf(height, MAX_BLUR_HASH_DIMEN), 1.0)
        }
    } catch (e: Exception) {
        null
    }
}

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
        if (dot > 0 && dot < this.length - 1) {
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

val MediaPath by lazy {
    MixinApplication.appContext.getMediaPath()?.toUri()?.toString()
}

val oldMediaPath by lazy {
    MixinApplication.appContext.getMediaPath(true)?.toUri()?.toString()
}

val ancientMediaPath by lazy {
    MixinApplication.appContext.getAncientMediaPath()?.toUri()?.toString()
}
