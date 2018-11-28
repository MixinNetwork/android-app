package one.mixin.android.job

import android.annotation.SuppressLint
import com.birbit.android.jobqueue.Params
import com.google.firebase.iid.FirebaseInstanceId
import io.reactivex.schedulers.Schedulers
import one.mixin.android.api.request.SessionRequest

class RefreshFcmTokenJob(private val token: String? = null)
    : BaseJob(Params(PRIORITY_BACKGROUND).addTags("RefreshFcmTokenJob").requireNetwork()) {

    companion object {
        private const val serialVersionUID = 1L
    }

    @SuppressLint("CheckResult")
    override fun onRun() {
        if (token != null) {
            accountService.updateSession(SessionRequest(notificationToken = token))
                .observeOn(Schedulers.io()).subscribe({}, {})
        } else {
            FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener {
                accountService.updateSession(SessionRequest(notificationToken = it.token))
                    .observeOn(Schedulers.io()).subscribe({}, {})
            }
        }
    }
}
