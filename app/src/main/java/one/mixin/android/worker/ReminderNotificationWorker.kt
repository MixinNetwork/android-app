package one.mixin.android.worker

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.extension.createBlazeNotification
import one.mixin.android.extension.notificationManager
import one.mixin.android.job.BlazeMessageService
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ReminderNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
) : BaseWork(context, parameters) {
    override suspend fun onRun(): Result {
        Timber.d("@@@ onRun")
        if (BlazeMessageService.isRunning(applicationContext)) return Result.success()

        Timber.d("@@@ createNotification")
        val notification = applicationContext.createBlazeNotification()
        applicationContext.notificationManager.notify(BlazeMessageService.FOREGROUND_ID, notification)
        return Result.success()
    }

    companion object {
        private const val TAG = "ReminderNotificationWorker"

        fun schedule(context: Context) {
            Timber.d("@@@ schedule Reminder work")
            val request = PeriodicWorkRequestBuilder<ReminderNotificationWorker>(20, TimeUnit.MINUTES)
                .addTag(TAG)
                // .setInitialDelay(20, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    TAG,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    request
                )
        }
    }
}
