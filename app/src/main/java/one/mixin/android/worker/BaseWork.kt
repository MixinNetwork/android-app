package one.mixin.android.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import okhttp3.OkHttpClient
import one.mixin.android.api.ClientErrorException
import one.mixin.android.api.LocalJobException
import one.mixin.android.api.NetworkException
import one.mixin.android.api.ServerErrorException
import one.mixin.android.api.WebSocketException
import one.mixin.android.di.worker.AndroidWorkerInjector
import java.net.SocketTimeoutException
import javax.inject.Inject

abstract class BaseWork(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    init {
        AndroidWorkerInjector.inject(this)
    }

    override fun doWork(): Result {
        return try {
            onRun()
        } catch (e: Exception) {
            if (shouldRetry(e)) {
                Result.RETRY
            } else {
                Result.FAILURE
            }
        }
    }

    abstract fun onRun(): Result

    private fun shouldRetry(throwable: Throwable): Boolean {
        if (throwable is SocketTimeoutException) {
            okHttpClient.connectionPool().evictAll()
            return true
        }
        return (throwable as? ServerErrorException)?.shouldRetry()
            ?: ((throwable as? ClientErrorException)?.shouldRetry()
                ?: ((throwable as? NetworkException)?.shouldRetry()
                    ?: ((throwable as? WebSocketException)?.shouldRetry()
                        ?: ((throwable as? LocalJobException)?.shouldRetry() ?: false))))
    }
}
