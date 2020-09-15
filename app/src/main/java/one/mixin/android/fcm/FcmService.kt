package one.mixin.android.fcm

import android.util.Log
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.job.MixinJobManager
import one.mixin.android.util.Session
import one.mixin.android.worker.RefreshFcmWorker
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FcmService", "From: ${remoteMessage.from}")
    }

    override fun onNewToken(token: String) {
        if (Session.checkToken()) {
            WorkManager.getInstance(this).enqueueOneTimeNetworkWorkRequest<RefreshFcmWorker>(
                workDataOf(RefreshFcmWorker.TOKEN to token)
            )
        }
    }
}
