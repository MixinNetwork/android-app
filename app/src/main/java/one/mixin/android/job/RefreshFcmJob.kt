package one.mixin.android.job

import android.annotation.SuppressLint
import com.birbit.android.jobqueue.Params
import com.google.firebase.messaging.FirebaseMessaging
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.request.SessionRequest

class RefreshFcmJob(
    private val token: String? = null,
) : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).requireNetwork().persist()) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshFcmJob"
    }

    @SuppressLint("CheckResult")
    override fun onRun() {
        if (token != null) {
            accountService.updateSession(SessionRequest(notificationToken = token))
                .observeOn(Schedulers.io()).subscribe({}, {})
        } else {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                accountService.updateSession(SessionRequest(notificationToken = token))
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io()).subscribe({}, {})
            }.addOnFailureListener {
                accountService.updateSession(SessionRequest())
                    .subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io()).subscribe({}, {})
            }
        }
    }
}
