package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.concurrent.TimeUnit

class UserBatchProcessorJob(
    private val userIds: List<String>
) : BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).requireNetwork().persist()) {

    companion object {
        private const val serialVersionUID = 1L
        private const val GROUP = "UserBatchProcessorJob"
        private const val FETCH_INTERVAL_HOURS = 24L
    }

    override fun onRun() = runBlocking {
        val filterId = userDao.filterUserIdsNotFetchedIn24Hours(userIds)
        (userIds - filterId).let { ids ->
            if (ids.isNotEmpty()) {
                jobManager.addJobInBackground(RefreshUserJob(ids, forceRefresh = true))
            }
        }
    }
}
