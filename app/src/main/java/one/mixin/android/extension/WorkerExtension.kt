package one.mixin.android.extension

import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

inline fun <reified W : ListenableWorker> WorkManager.enqueueOneTimeNetworkWorkRequest(inputData: Data? = null) {
    enqueue(OneTimeWorkRequestBuilder<W>()
        .apply {
            if (inputData != null) {
                setInputData(inputData)
            }
        }
        .setConstraints(Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build())
        .build())
}