package one.mixin.android.util.image

import android.content.Context
import androidx.annotation.WorkerThread
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class NetworkFetcher private constructor(
    context: Context,
    private val url: String
) {
    private val networkCache: NetworkCache = NetworkCache(context)

    companion object {
        fun fetchSync(context: Context, url: String): ImageResult<File> =
            NetworkFetcher(context, url).fetchSync()
    }

    @WorkerThread
    fun fetchSync(): ImageResult<File> {
        val result = fetchFromCache()
        if (result != null) {
            return ImageResult(result)
        }
        Timber.d("Animation for $url not found in cache. Fetching from network.")
        return fetchFromNetwork()
    }

    private fun fetchFromCache() = networkCache.fetch(url)

    private fun fetchFromNetwork(): ImageResult<File> {
        return try {
            fetchFromNetworkInternal()
        } catch (e: IOException) {
            ImageResult(exception = e)
        }
    }

    private fun fetchFromNetworkInternal(): ImageResult<File> {
        Timber.d("Fetching $url")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
        }
        try {
            connection.connect()
            if (connection.errorStream != null || connection.responseCode != HttpURLConnection.HTTP_OK) {
                val error = getErrorFromConnection(connection)
                return ImageResult(exception = IllegalArgumentException("Unable to fetch $url. Failed with ${connection.responseCode}\n$error"))
            }
            val result = getResultFromConnection(connection)
            Timber.d("Completed fetch from network. Success: ${result.value != null}")
            return result
        } catch (e: Exception) {
            return ImageResult(exception = e)
        } finally {
            connection.disconnect()
        }
    }

    private fun getErrorFromConnection(connection: HttpURLConnection): String {
        val error = StringBuilder()
        connection.errorStream.bufferedReader().useLines {
            error.append(it).append('\n')
        }
        return error.toString()
    }

    private fun getResultFromConnection(connection: HttpURLConnection): ImageResult<File> {
        return getResultFromStream(connection.inputStream)
    }

    private fun getResultFromStream(inputStream: InputStream): ImageResult<File> {
        val file = networkCache.writeTempCacheFile(url, inputStream)
        val result = ImageResult(file)
        if (result.value != null) {
            networkCache.renameTempFile(url)
        }
        return result
    }
}
