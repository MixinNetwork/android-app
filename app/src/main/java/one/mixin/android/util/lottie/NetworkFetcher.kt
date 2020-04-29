package one.mixin.android.util.lottie

import android.content.Context
import androidx.annotation.WorkerThread
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import timber.log.Timber

class NetworkFetcher private constructor(
    context: Context,
    private val url: String
) {
    private val networkCache: NetworkCache = NetworkCache(context)

    companion object {
        fun fetchSync(context: Context, url: String): LottieResult<File> =
            NetworkFetcher(context, url).fetchSync()
    }

    @WorkerThread
    fun fetchSync(): LottieResult<File> {
        val result = fetchFromCache()
        if (result != null) {
            return LottieResult(result)
        }
        Timber.d("Animation for $url not found in cache. Fetching from network.")
        return fetchFromNetwork()
    }

    private fun fetchFromCache() = networkCache.fetch(url)

    private fun fetchFromNetwork(): LottieResult<File> {
        return try {
            fetchFromNetworkInternal()
        } catch (e: IOException) {
            LottieResult(exception = e)
        }
    }

    private fun fetchFromNetworkInternal(): LottieResult<File> {
        Timber.d("Fetching $url")
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
        }
        try {
            connection.connect()
            if (connection.errorStream != null || connection.responseCode != HttpURLConnection.HTTP_OK) {
                val error = getErrorFromConnection(connection)
                return LottieResult(exception = IllegalArgumentException("Unable to fetch $url. Failed with ${connection.responseCode}\n$error"))
            }
            val result = getResultFromConnection(connection)
            Timber.d("Completed fetch from network. Success: ${result.value != null}")
            return result
        } catch (e: Exception) {
            return LottieResult(exception = e)
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

    private fun getResultFromConnection(connection: HttpURLConnection): LottieResult<File> {
        val file = networkCache.writeTempCacheFile(url, connection.inputStream)
        val result = LottieResult(file)
        if (result.value != null) {
            networkCache.renameTempFile(url)
        }
        return result
    }
}
