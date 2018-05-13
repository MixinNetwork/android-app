package one.mixin.android.fcm

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import dagger.android.AndroidInjection
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.RefreshFcmTokenJob
import one.mixin.android.util.Session
import javax.inject.Inject

class FcmInstanceIDService : FirebaseInstanceIdService() {

    @Inject
    lateinit var jobManager: MixinJobManager

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onTokenRefresh() {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        sendRegistrationToServer(refreshedToken)
    }

    private fun sendRegistrationToServer(token: String?) {
        if (Session.hasToken()) {
            jobManager.addJobInBackground(RefreshFcmTokenJob(token))
        }
    }
}
