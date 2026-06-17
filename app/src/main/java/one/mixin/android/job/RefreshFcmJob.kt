package one.mixin.android.job

import android.annotation.SuppressLint
import com.birbit.android.jobqueue.Params

class RefreshFcmJob(
    private val notificationToken: String? = null,
    private val deviceCheckToken: String? = null,
) : BaseJob(Params(PRIORITY_UI_HIGH).setSingleId(GROUP).requireNetwork()) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshFcmJob"
    }

    @SuppressLint("CheckResult")
    override fun onRun() {
       // do nothing
    }
}
