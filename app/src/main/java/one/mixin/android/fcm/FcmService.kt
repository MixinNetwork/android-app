package one.mixin.android.fcm

import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.extension.enqueueOneTimeNetworkWorkRequest
import one.mixin.android.job.MixinJobManager
import one.mixin.android.session.Session
import one.mixin.android.worker.RefreshFcmWorker
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Timber.d("From: " + remoteMessage.from)
    }

    override fun onNewToken(token: String) {
        if (Session.checkToken()) {
            WorkManager.getInstance(this).enqueueOneTimeNetworkWorkRequest<RefreshFcmWorker>(
                workDataOf(RefreshFcmWorker.TOKEN to token)
            )
        }
    }
}
