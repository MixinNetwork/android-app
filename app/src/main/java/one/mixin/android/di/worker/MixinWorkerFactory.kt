package one.mixin.android.di.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import javax.inject.Inject
import javax.inject.Provider

class MixinWorkerFactory @Inject constructor(
    private val creators: Map<Class<out ListenableWorker>,
        @JvmSuppressWildcards Provider<ChildWorkerFactory>>
) : WorkerFactory() {
    override fun createWorker(
        context: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        val workerClass = Class.forName(workerClassName)
        val foundEntry = creators.entries.find { workerClass.isAssignableFrom(it.key) }
        val factory = foundEntry?.value
            ?: throw IllegalArgumentException("unknown worker class name: $workerClassName")
        return factory.get().create(context, workerParameters)
    }
}
