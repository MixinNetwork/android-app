package one.mixin.android.util.lottie

import android.os.Handler
import android.os.Looper
import java.util.ArrayList
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import timber.log.Timber

class LottieTask<T>(
    runnable: Callable<LottieResult<T>>,
    runNow: Boolean = false
) {
    private val executor = Executors.newCachedThreadPool()
    private val successListeners = LinkedHashSet<LottieListener<T>>(1)
    private val failureListeners = LinkedHashSet<LottieListener<Throwable>>(1)
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    private var result: LottieResult<T>? = null

    init {
        if (runNow) {
            try {
                setResult(runnable.call())
            } catch (e: Exception) {
                setResult(LottieResult(exception = e))
            }
        } else {
            executor.execute(LottieFutureTask(runnable))
        }
    }

    private fun setResult(result: LottieResult<T>) {
        if (this.result != null) {
            throw IllegalStateException("A task may only be set once.")
        }
        this.result = result
        notifyListeners()
    }

    @Synchronized
    fun addListener(listener: LottieListener<T>): LottieTask<T>? {
        result?.value?.let { listener.onResult(it) }
        successListeners.add(listener)
        return this
    }

    @Synchronized
    fun removeListener(listener: LottieListener<T>?): LottieTask<T>? {
        successListeners.remove(listener)
        return this
    }

    @Synchronized
    fun addFailureListener(listener: LottieListener<Throwable>): LottieTask<T>? {
        result?.exception?.let { listener.onResult(it) }
        failureListeners.add(listener)
        return this
    }

    @Synchronized
    fun removeFailureListener(listener: LottieListener<Throwable>): LottieTask<T>? {
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
        val listenersCopy: List<LottieListener<T>> = ArrayList(successListeners)
        for (l in listenersCopy) {
            l.onResult(value)
        }
    }

    @Synchronized
    private fun notifyFailureListeners(e: Throwable) {
        val listenersCopy: List<LottieListener<Throwable>> = ArrayList(failureListeners)
        if (listenersCopy.isEmpty()) {
            Timber.w("Lottie encountered an error but no failure listener was added: $e")
            return
        }
        for (l in listenersCopy) {
            l.onResult(e)
        }
    }

    inner class LottieFutureTask(
        callable: Callable<LottieResult<T>>
    ) : FutureTask<LottieResult<T>>(callable) {
        override fun done() {
            if (isCancelled) return

            try {
                setResult(get())
            } catch (e: Exception) {
                when (e) {
                    is InterruptedException, is ExecutionException -> {
                        setResult(LottieResult(exception = e))
                    }
                    else -> throw e
                }
            }
        }
    }
}
