package one.mixin.android.di.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters

interface ChildWorkerFactory {
    fun create(context: Context, parameters: WorkerParameters): ListenableWorker
}
