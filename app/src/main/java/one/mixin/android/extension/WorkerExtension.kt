package one.mixin.android.extension

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkRequest.MIN_BACKOFF_MILLIS
import java.util.concurrent.TimeUnit

inline fun <reified W : ListenableWorker> WorkManager.enqueueOneTimeNetworkWorkRequest(inputData: Data? = null) {
    enqueue(OneTimeWorkRequestBuilder<W>()
        .apply {
            if (inputData != null) {
                setInputData(inputData)
            }
        }
        .setBackoffCriteria(BackoffPolicy.LINEAR, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build())
        .build())
}

inline fun <reified W : ListenableWorker> WorkManager.enqueueOneTimeRequest(inputData: Data? = null) {
    enqueue(OneTimeWorkRequestBuilder<W>()
        .setBackoffCriteria(BackoffPolicy.LINEAR, MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
        .apply {
            if (inputData != null) {
                setInputData(inputData)
            }
        }.build())
}