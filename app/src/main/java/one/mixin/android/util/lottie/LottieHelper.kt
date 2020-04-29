package one.mixin.android.util.lottie

import android.content.Context
import androidx.annotation.WorkerThread
import java.io.File
import java.util.concurrent.Callable

object LottieHelper {
    private val taskCache = HashMap<String, LottieTask<File>>()

    @WorkerThread
    fun fromUrlSync(context: Context, url: String) =
        NetworkFetcher.fetchSync(context, url)

    fun fromUrl(context: Context, url: String, cacheKey: String? = url): LottieTask<File> {
        return cache(cacheKey, Callable { NetworkFetcher.fetchSync(context, url) })
    }

    private fun cache(cacheKey: String?, callable: Callable<LottieResult<File>>): LottieTask<File> {
        if (cacheKey != null && taskCache.containsKey(cacheKey)) {
            val cachedTask = taskCache[cacheKey]
            if (cachedTask != null) {
                return cachedTask
            }
        }

        val task = LottieTask(callable)
        if (cacheKey != null) {
            task.addListener(object : LottieListener<File> {
                override fun onResult(result: File) {
                    taskCache.remove(cacheKey)
                }
            })
            task.addFailureListener(object : LottieListener<Throwable> {
                override fun onResult(result: Throwable) {
                    taskCache.remove(cacheKey)
                }
            })
            taskCache[cacheKey] = task
        }
        return task
    }
}

const val TEMP_JSON_EXTENSION = ".temp.json"
const val JSON_EXTENSION = ".json"
