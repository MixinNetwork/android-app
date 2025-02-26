package one.mixin.android.job

import android.annotation.SuppressLint
import com.birbit.android.jobqueue.Params
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.runBlocking
import okhttp3.internal.wait
import one.mixin.android.api.request.SessionRequest
import timber.log.Timber

class RefreshFcmJob(
    private val notificationToken: String? = null,
    private val deviceCheckToken: String? = null,
) : BaseJob(Params(PRIORITY_UI_HIGH).setSingleId(GROUP).requireNetwork()) {
    companion object {
        private const val serialVersionUID = 2L
        const val GROUP = "RefreshFcmJob"
    }

    @SuppressLint("CheckResult")
    override fun onRun() {
       // do nothing
    }
}
