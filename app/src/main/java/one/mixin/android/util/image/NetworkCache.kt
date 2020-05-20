package one.mixin.android.util.image

import android.content.Context
import java.io.File
import java.io.InputStream
import timber.log.Timber

class NetworkCache(context: Context) {
    private val appContext = context.applicationContext

    fun fetch(url: String): File? {
        val cachedFile = getCachedFile(url) ?: return null
        Timber.d("Cache hit for $url at ${cachedFile.absolutePath}")
        return cachedFile
    }

    fun writeTempCacheFile(url: String, stream: InputStream): File {
        val fileName = filenameForUrl(url, true)
        val file = File(parentDir(), fileName)
        file.outputStream().use {
            stream.copyTo(it)
        }
        return file
    }

    fun renameTempFile(url: String) {
        val fileName = filenameForUrl(url, true)
        val file = File(parentDir(), fileName)
        val newFileName = file.absolutePath.replace(".temp", "")
        val newFile = File(newFileName)
        val renamed = file.renameTo(newFile)
        Timber.d("Copying temp file to real file ($newFile)")
        if (!renamed) {
            Timber.w("Unable to rename cache file ${file.absolutePath} to ${newFile.absolutePath}.")
        }
    }

    fun clear() {
        val parentDir = parentDir()
        if (parentDir.exists()) {
            val files = parentDir.listFiles()
            if (!files.isNullOrEmpty()) {
                for (f in files) {
                    f.delete()
                }
            }
            parentDir.delete()
        }
    }

    private fun getCachedFile(url: String): File? {
        val file = File(parentDir(), filenameForUrl(url, false))
        return if (file.exists()) {
            file
        } else null
    }

    private fun parentDir(): File {
        val file = File(appContext.cacheDir, "lottie_network_cache")
        if (file.isFile) {
            file.delete()
        }
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    companion object {
        fun filenameForUrl(url: String, isTemp: Boolean) =
            "lottie_cache_" + url.replace("\\W+".toRegex(), "") + if (isTemp) TEMP_JSON_EXTENSION else JSON_EXTENSION
    }
}
