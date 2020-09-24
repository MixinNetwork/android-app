package one.mixin.android.util

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.DIRECTORY_MOVIES
import android.os.Environment.DIRECTORY_MUSIC
import android.os.Environment.DIRECTORY_PICTURES
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import one.mixin.android.MixinApplication
import one.mixin.android.extension.copyFromInputStream
import one.mixin.android.extension.getExtensionName
import one.mixin.android.extension.getLegacyAudioPath
import one.mixin.android.extension.getLegacyBackupPath
import one.mixin.android.extension.getLegacyDocumentPath
import one.mixin.android.extension.getLegacyImagePath
import one.mixin.android.extension.getLegacyOtherPath
import one.mixin.android.extension.getLegacyVideoPath
import one.mixin.android.extension.isImageSupport
import one.mixin.android.session.Session
import one.mixin.android.widget.gallery.MimeType
import java.io.File
import java.io.IOException
import java.io.InputStream

fun getBackupPath(): File? {
    return if (isSupportQ()) {
        val storage = Environment.getExternalStorageDirectory()
        val identityNumber = Session.getAccount()?.identityNumber
        File("${storage.absolutePath}${File.separator}Mixin${File.separator}$identityNumber${File.separator}Backup")
    } else {
        MixinApplication.appContext.getLegacyBackupPath(false)
    }
}

fun getImagesUri(): Uri {
    return if (isSupportQ()) {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    } else {
        MixinApplication.appContext.getLegacyImagePath().uri()
    }
}

fun getAudioUri(): Uri {
    return if (isSupportQ()) {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    } else {
        MixinApplication.appContext.getLegacyAudioPath().uri()
    }
}

fun getVideoUri(): Uri {
    return if (isSupportQ()) {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    } else {
        MixinApplication.appContext.getLegacyVideoPath().uri()
    }
}

fun getFilesUri(): Uri {
    return if (isSupportQ()) {
        MediaStore.Files.getContentUri("external")
    } else {
        MixinApplication.appContext.getLegacyDocumentPath().uri()
    }
}

fun getDownloadUri(): Uri {
    return if (isSupportQ()) {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    } else {
        MixinApplication.appContext.getLegacyOtherPath().uri()
    }
}

@Suppress("DEPRECATION")
private fun getLegacyUri(directory: String): Uri =
    Uri.fromFile(Environment.getExternalStoragePublicDirectory(directory))

fun getMediaStoreImagesUri() = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

fun getMediaStoreAudioUri() = if (isSupportQ()) {
    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
} else {
    getLegacyUri(DIRECTORY_MUSIC)
}

fun getMediaStoreVideoUri() = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

// fun getMediaStoreFilesUri() = MediaStore.Files.getContentUri("external")

fun getMediaStoreDownloadUri() = if (isSupportQ()) {
    MediaStore.Downloads.EXTERNAL_CONTENT_URI
} else {
    getLegacyUri(DIRECTORY_DOWNLOADS)
}

fun hasWriteMediaStorePermission() = Build.VERSION.SDK_INT > 28 ||
    ContextCompat.checkSelfPermission(
    MixinApplication.appContext,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
) == PackageManager.PERMISSION_GRANTED

fun hasReadMediaStorePermission() = Build.VERSION.SDK_INT > 28 ||
    ContextCompat.checkSelfPermission(
    MixinApplication.appContext,
    Manifest.permission.READ_EXTERNAL_STORAGE
) == PackageManager.PERMISSION_GRANTED

fun hasRWMediaStorePermission() = hasReadMediaStorePermission() && hasWriteMediaStorePermission()

fun getImagesRelativePath(conversationId: String) = "$DIRECTORY_PICTURES${File.separator}Mixin${File.separator}$conversationId"
fun getAudioRelativePath(conversationId: String) = "$DIRECTORY_MUSIC${File.separator}Mixin${File.separator}$conversationId"
fun getVideoRelativePath(conversationId: String) = "$DIRECTORY_MOVIES${File.separator}Mixin${File.separator}$conversationId"
// fun getFilesRelativePath(conversationId: String) = "$DIRECTORY_DOCUMENTS${File.separator}Mixin${File.separator}$conversationId"
fun getDownloadRelativePath(conversationId: String) = "$DIRECTORY_DOWNLOADS${File.separator}Mixin${File.separator}$conversationId"

private fun File.uri() = Uri.fromFile(this)

fun isSupportQ() = Build.VERSION.SDK_INT >= 29

fun copyInputStreamToUri(uri: Uri, inputStream: InputStream) {
    uri.copyFromInputStream(inputStream)
    if (isSupportQ()) {
        val updatePendingValues = ContentValues().apply {
            put(MediaStore.MediaColumns.IS_PENDING, 0)
        }
        MixinApplication.appContext.contentResolver.update(uri, updatePendingValues, null, null)
    }
}

@Throws(IOException::class)
fun createMediaStoreUri(outputUri: Uri, relativePath: String, fileName: String, ext: String): Uri? {
    val path = outputUri.path ?: return null

    val mimeTypeMap = MimeTypeMap.getSingleton()
    val mimeType = mimeTypeMap.getMimeTypeFromExtension(ext)
    val fileParts = getFileNameParts(fileName)
    val base = fileParts[0]
    val extension = fileParts[1]
    val contentValue = ContentValues().apply {
        if (isSupportQ()) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
        }
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis())
        put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis())

        if (isSupportQ()) {
            put(MediaStore.MediaColumns.IS_PENDING, true)
        }

        if (!isSupportQ() && outputUri.scheme == ContentResolver.SCHEME_FILE) {
            val outputDirectory = File(path)
            var outFile = File(outputDirectory, "$base.$extension")
            var i = 0
            while (outFile.exists()) {
                outFile = File(outputDirectory, "$base-(${++i}).$extension")
            }

            if (outFile.isHidden) {
                throw IOException("Specified name would not be visible")
            }
            return Uri.fromFile(outFile)
        }
    }
    return MixinApplication.appContext.contentResolver.insert(outputUri, contentValue)
}

fun getAttachmentImagesUri(mediaMimeType: String?, conversationId: String, messageId: String, name: String?): Uri? {
    val outputUri = getMediaStoreImagesUri()
    val relativePath = getImagesRelativePath(conversationId)
    return when {
        mediaMimeType?.isImageSupport() == false -> {
            createMediaStoreUri(outputUri, relativePath, name ?: messageId, "")
        }
        mediaMimeType.equals(MimeType.PNG.toString(), true) -> {
            createMediaStoreUri(outputUri, relativePath, name ?: messageId, ".png")
        }
        mediaMimeType.equals(MimeType.GIF.toString(), true) -> {
            createMediaStoreUri(outputUri, relativePath, name ?: messageId, ".gif")
        }
        mediaMimeType.equals(MimeType.WEBP.toString(), true) -> {
            createMediaStoreUri(outputUri, relativePath, name ?: messageId, ".webp")
        }
        else -> {
            createMediaStoreUri(outputUri, relativePath, name ?: messageId, ".jpg")
        }
    }
}

fun getAttachmentVideoUri(conversationId: String, messageId: String, name: String?): Uri? {
    val outputUri = getMediaStoreVideoUri()
    val extensionName = name?.getExtensionName().let {
        it ?: "mp4"
    }
    return createMediaStoreUri(outputUri, getVideoRelativePath(conversationId), name ?: messageId, extensionName)
}

fun getAttachmentAudioUri(conversationId: String, messageId: String, name: String?): Uri? {
    val outputUri = getMediaStoreAudioUri()
    return createMediaStoreUri(outputUri, getAudioRelativePath(conversationId), name ?: messageId, "ogg")
}

fun getAttachmentFilesUri(conversationId: String, messageId: String, name: String?): Uri? {
    val outputUri = getMediaStoreDownloadUri()
    val extensionName = name?.getExtensionName().let { it ?: "attach" }
    return createMediaStoreUri(outputUri, getDownloadRelativePath(conversationId), name ?: messageId, extensionName)
}

private fun getFileNameParts(fileName: String): Array<String?> {
    val result = arrayOfNulls<String>(2)
    val tokens = fileName.split("\\.(?=[^.]+$)".toRegex())
    if (tokens.isNullOrEmpty()) {
        return result
    }
    result[0] = tokens[0]
    if (tokens.size > 1) result[1] = tokens[1] else result[1] = ""
    return result
}
