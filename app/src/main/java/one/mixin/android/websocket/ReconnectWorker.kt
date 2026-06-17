package one.mixin.android.websocket

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import one.mixin.android.job.BlazeMessageService
import one.mixin.android.job.BlazeMessageService.Companion.ACTION_ACTIVITY_PAUSE
import one.mixin.android.worker.BaseWork

@HiltWorker
class ReconnectWorker @AssistedInject constructor(
        @Assisted val context: Context,
        @Assisted parameters: WorkerParameters,
        private val chatWebSocket: ChatWebSocket,
    ) : BaseWork(context, parameters) {

    override suspend fun onRun(): Result {
        if (chatWebSocket.connected) {
            return Result.success()
        }
        BlazeMessageService.startService(context, ACTION_ACTIVITY_PAUSE)
        return Result.success()
    }
}