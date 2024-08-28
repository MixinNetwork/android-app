package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking

class RefreshMarketsJob() : BaseJob(
    Params(PRIORITY_UI_HIGH)
        .addTags(GROUP).requireNetwork(),
) {
    companion object {
        private const val serialVersionUID = 1L
        const val GROUP = "RefreshMarketsJob"
    }

    override fun onRun() = runBlocking{
        val response = routeService.markets()
        if (response.isSuccess && response.data != null) {
            response.data?.let {
                marketDao.insertList(it)
            }
        }
    }
}
