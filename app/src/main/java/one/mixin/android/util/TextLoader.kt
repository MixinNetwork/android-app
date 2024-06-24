import android.content.Context
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import one.mixin.android.MixinApplication
import timber.log.Timber
import java.io.File
import java.io.IOException

class TextLoader(context: Context) {
    companion object {
        private const val TAG = "TextLoader"
        private const val CACHE_DIR = "network_text_cache"
        private const val CACHE_SIZE = 10 * 1024 * 1024L // 10MB
    }

    private val okHttpClient: OkHttpClient

    init {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        val cache = Cache(cacheDir, CACHE_SIZE)
        okHttpClient = OkHttpClient.Builder()
            .cache(cache)
            .build()
    }

    suspend fun getData(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()
                } else {
                    null
                }
            } catch (e: IOException) {
                Timber.e(TAG, "Error fetching data", e)
                null
            }
        }
    }
}

private val textLoader by lazy {
    TextLoader(MixinApplication.appContext)
}

fun TextView.load(url: String) {
    CoroutineScope(Dispatchers.Main).launch {
        text = textLoader.getData(url)
    }
}