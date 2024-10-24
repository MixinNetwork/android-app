package one.mixin.android.util

import android.content.Context
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import one.mixin.android.MixinApplication
import one.mixin.android.ui.home.inscription.component.AutoSizeConstraint
import one.mixin.android.ui.home.inscription.component.AutoSizeText
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

    suspend fun getData(url: String?): String? {
        url ?: return ""
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val contentType = response.header("Content-Type") ?: ""
                    if (contentType.matches(Regex("^text/.*"))) {
                        response.body?.string()?.stripInvisibleCharacters()
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: IOException) {
                Timber.e(TAG, "Error fetching data", e)
                null
            }
        }
    }

    private fun String.stripInvisibleCharacters(): String {
        return this.trim().replace(Regex("[\\s\\p{Cf}\\p{Cc}\\p{Cn}]"), "■")
    }
}

private val textLoader by lazy {
    TextLoader(MixinApplication.appContext)
}

fun TextView.load(url: String?) {
    CoroutineScope(Dispatchers.Main).launch {
        text = textLoader.getData(url)
    }
}

@Composable
fun TextLoaderComposable(url: String?) {
    url ?: return
    val context = LocalContext.current
    val textLoader = remember { TextLoader(context) }
    var text by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        text = textLoader.getData(url)
    }

    AutoSizeText(
        text = text ?: "", maxLines = 12, color = Color(0xFF, 0xA7, 0x24, 0xFF), fontSize = 24.sp, constraint = AutoSizeConstraint.Height(min = 12.sp), overflow = TextOverflow.Ellipsis
    )
}
