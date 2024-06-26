package one.mixin.android.util

import android.content.Context
import android.widget.TextView
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    suspend fun getData(url: String?): String? {
        url ?: return ""
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
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(url) {
        isLoading = true
        text = textLoader.getData(url)
        isLoading = false
    }

    if (isLoading) {
        CircularProgressIndicator(
            modifier =
            Modifier.size(10.dp),
            color = Color.White,
        )
    } else {
        if (!text.isNullOrEmpty()) AutoSizeTextView(text = text ?: "", maxLines = 12, color = Color(0xFF, 0xA7, 0x24, 0xFF))
    }
}


@Composable
fun AutoSizeTextView(
    text: String,
    maxLines: Int,
    color: Color,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 12.sp,
    maxFontSize: TextUnit = 24.sp
) {
    var textSize by remember { mutableStateOf(maxFontSize) }
    var readyToDraw by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        val boxWidth = maxWidth
        val boxHeight = maxHeight
        val density = LocalDensity.current

        Text(
            text = text,
            color = color,
            maxLines = maxLines,
            modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
                val textWidth = layoutCoordinates.size.width
                val textHeight = layoutCoordinates.size.height
                if (textWidth > with(density) { boxWidth.toPx() } || textHeight > with(density) { boxHeight.toPx() }) {
                    textSize = if (textSize > minFontSize) {
                        (textSize.value - 1).sp
                    } else {
                        readyToDraw = true
                        textSize
                    }
                } else {
                    readyToDraw = true
                }
            }
        )

        if (!readyToDraw) {
            Text(text = text, color = color, maxLines = maxLines)
        }
    }
}
