package one.mixin.android.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
import android.webkit.MimeTypeMap
import androidx.core.content.IntentCompat
import androidx.core.net.toUri
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import one.mixin.android.extension.getFileName
import one.mixin.android.vo.ForwardCategory
import one.mixin.android.vo.ForwardMessage
import one.mixin.android.vo.ShareCategory
import one.mixin.android.vo.ShareImageData
import one.mixin.android.websocket.DataMessagePayload
import one.mixin.android.websocket.VideoMessagePayload
import java.io.File
import java.io.InputStream
import java.io.OutputStream

class ShareHelper {
    companion object {
        internal const val STAGING_DIRECTORY = "external-share"
        private const val JOB_DIRECTORY = "external-share-jobs"
        private const val MAX_SHARE_ITEMS = 32
        private const val MAX_SHARE_BYTES = 1024 * 1024 * 1024L

        @Volatile
        private var INSTANCE: ShareHelper? = null

        fun get(): ShareHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShareHelper().also { INSTANCE = it }
            }

        fun retainJobSource(context: Context, uri: Uri): Uri {
            if (uri.scheme != ContentResolver.SCHEME_FILE) return uri
            val source = File(uri.path ?: return uri).canonicalFile
            if (source.parentFile?.parentFile != File(context.cacheDir, STAGING_DIRECTORY).canonicalFile) return uri
            val directory = File(context.filesDir, JOB_DIRECTORY).apply { mkdirs() }
            val target = File.createTempFile("share-", ".${source.extension}", directory)
            try {
                source.copyTo(target, overwrite = true)
                return target.toUri()
            } catch (e: Exception) {
                target.delete()
                throw e
            }
        }

        fun releaseJobSource(context: Context, uri: Uri) {
            if (uri.scheme != ContentResolver.SCHEME_FILE) return
            val source = File(uri.path ?: return).canonicalFile
            if (source.parentFile == File(context.filesDir, JOB_DIRECTORY).canonicalFile) source.delete()
        }
    }

    suspend fun generateForwardMessageList(context: Context, intent: Intent, directory: File): ArrayList<ForwardMessage>? {
        val action = intent.action
        val type = intent.type ?: return null
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return null
        if (action == Intent.ACTION_SEND && type == "text/plain") {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.takeIf { it.isNotEmpty() }?.let {
                return arrayListOf(ForwardMessage(ShareCategory.Text, it))
            }
        }
        val clipData = intent.clipData
        require((clipData?.itemCount ?: 0) <= MAX_SHARE_ITEMS)
        val clipUris = (0 until (clipData?.itemCount ?: 0)).mapNotNull { clipData?.getItemAt(it)?.uri }
        val streams = if (action == Intent.ACTION_SEND) {
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { listOf(it) }
        } else {
            IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        }
        val uris = streams ?: clipUris
        require(uris.isNotEmpty() && uris.size <= MAX_SHARE_ITEMS)
        require(action != Intent.ACTION_SEND || uris.size == 1)
        require(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
        uris.forEach { uri ->
            require(clipData == null || uri in clipUris)
            require(uri.scheme == ContentResolver.SCHEME_CONTENT)
            val authority = uri.authority?.substringAfterLast('@') ?: throw IllegalArgumentException()
            require(!authority.equals("${context.packageName}.provider", ignoreCase = true))
            val provider = context.packageManager.resolveContentProvider(authority, 0) ?: throw IllegalArgumentException()
            require(provider.applicationInfo.uid != context.applicationInfo.uid)
            require(context.checkUriPermission(uri, Process.myPid(), Process.myUid(), Intent.FLAG_GRANT_READ_URI_PERMISSION) == PackageManager.PERMISSION_GRANTED)
        }
        require(directory.mkdirs() || directory.isDirectory)
        val result = arrayListOf<ForwardMessage>()
        var remainingBytes = MAX_SHARE_BYTES
        try {
            uris.forEach { uri ->
                currentCoroutineContext().ensureActive()
                val name = uri.getFileName(context)
                val mime = context.contentResolver.getType(uri) ?: type
                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
                    ?: name.substringAfterLast('.', "").takeIf { it.matches(Regex("[a-zA-Z0-9]{1,10}")) }
                    ?: "bin"
                val file = File.createTempFile("share-", ".$extension", directory)
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input)
                    file.outputStream().use { output -> remainingBytes -= copyShareContent(input, output, remainingBytes) }
                }
                val localUri = file.toUri().toString()
                val message = when {
                    type.startsWith("image/") -> ForwardMessage(ShareCategory.Image, GsonHelper.customGson.toJson(ShareImageData(localUri)))
                    type.startsWith("video/") -> ForwardMessage(ForwardCategory.Video, GsonHelper.customGson.toJson(VideoMessagePayload(localUri)))
                    else -> ForwardMessage(ForwardCategory.Data, GsonHelper.customGson.toJson(DataMessagePayload(localUri, name, if ('*' in type) mime else type, file.length())))
                }
                result.add(message)
            }
            return result
        } catch (e: Exception) {
            directory.deleteRecursively()
            throw e
        }
    }
}

internal suspend fun copyShareContent(input: InputStream, output: OutputStream, maxBytes: Long): Long {
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val count = input.read(buffer)
        if (count == -1) return total
        require(count.toLong() <= maxBytes - total)
        output.write(buffer, 0, count)
        total += count
    }
}
