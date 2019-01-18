package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.util.Session

class RefreshSessionJob
    : BaseJob(Params(PRIORITY_UI_HIGH).addTags(RefreshSessionJob.GROUP).requireNetwork().persist()) {

    companion object {
        private const val serialVersionUID = 1L
        private const val GROUP = "RefreshSessionJob"
    }

    override fun onRun() {
        val call = accountService.getSessions(listOf(Session.getAccountId()!!)).execute()
        val response = call.body()
        if (response != null && response.isSuccess) {
            response.data?.let { sessions ->
                for (session in sessions) {
                    sessionDao.insert(session)
                }
            }
        }
    }
}
