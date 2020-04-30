package one.mixin.android.util.image

import android.os.Handler
import android.os.Looper
import java.util.ArrayList
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import timber.log.Timber

class ImageTask<T>(
    runnable: Callable<ImageResult<T>>,
    runNow: Boolean = false
) {
    private val executor = Executors.newCachedThreadPool()
    private val successListeners = LinkedHashSet<ImageListener<T>>(1)
    private val failureListeners = LinkedHashSet<ImageListener<Throwable>>(1)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var result: ImageResult<T>? = null

    init {
        if (runNow) {
            try {
                setResult(runnable.call())
            } catch (e: Exception) {
                setResult(ImageResult(exception = e))
            }
        } else {
            executor.execute(LottieFutureTask(runnable))
        }
    }

    private fun setResult(result: ImageResult<T>) {
        if (this.result != null) {
            throw IllegalStateException("A task may only be set once.")
        }
        this.result = result
        notifyListeners()
    }

    @Synchronized
    fun addListener(listener: ImageListener<T>): ImageTask<T>? {
        result?.value?.let { listener.onResult(it) }
        successListeners.add(listener)
        return this
    }

    @Synchronized
    fun removeListener(listener: ImageListener<T>?): ImageTask<T>? {
        successListeners.remove(listener)
        return this
    }

    @Synchronized
    fun addFailureListener(listener: ImageListener<Throwable>): ImageTask<T>? {
        result?.exception?.let { listener.onResult(it) }
        failureListeners.add(listener)
        return this
    }

    @Synchronized
    fun removeFailureListener(listener: ImageListener<Throwable>): ImageTask<T>? {
        failureListeners.remove(listener)
        return this
    }

    private fun notifyListeners() {
        handler.post(Runnable {
            if (result == null) {
                return@Runnable
            }
            result?.let { r ->
                if (r.value != null) {
                    notifySuccessListeners(r.value)
                } else {
                    r.exception?.let { notifyFailureListeners(it) }
                }
            }
        })
    }

    @Synchronized
    private fun notifySuccessListeners(value: T) {
        val listenersCopy: List<ImageListener<T>> = ArrayList(successListeners)
        for (l in listenersCopy) {
            l.onResult(value)
        }
    }

    @Synchronized
    private fun notifyFailureListeners(e: Throwable) {
        val listenersCopy: List<ImageListener<Throwable>> = ArrayList(failureListeners)
        if (listenersCopy.isEmpty()) {
            Timber.w("Lottie encountered an error but no failure listener was added: $e")
            return
        }
        for (l in listenersCopy) {
            l.onResult(e)
        }
    }

    inner class LottieFutureTask(
        callable: Callable<ImageResult<T>>
    ) : FutureTask<ImageResult<T>>(callable) {
        override fun done() {
            if (isCancelled) return

            try {
                setResult(get())
            } catch (e: Exception) {
                when (e) {
                    is InterruptedException, is ExecutionException -> {
                        setResult(ImageResult(exception = e))
                    }
                    else -> throw e
                }
            }
        }
    }
}
