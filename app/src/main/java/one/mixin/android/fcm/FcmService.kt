package one.mixin.android.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.android.AndroidInjection
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshFcmTokenJob
import one.mixin.android.util.Session
import javax.inject.Inject

class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
    }

    override fun onNewToken(token: String?) {
        if (Session.checkToken()) {
            jobManager.addJobInBackground(RefreshFcmTokenJob(token))
        }
    }
}
