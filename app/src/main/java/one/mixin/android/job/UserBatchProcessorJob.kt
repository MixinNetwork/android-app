package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class UserBatchProcessorJob(
    private val userIds: List<String>,
) : BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).requireNetwork().persist()) {

    companion object {
        private const val serialVersionUID = 1L
        private const val GROUP = "UserBatchProcessorJob"
        private val FETCH_INTERVAL_MILLIS = TimeUnit.HOURS.toMillis(24)
    }

    override fun onRun() = runBlocking {
        val fetchedUserIds =
            userDao.findFetchedUserIdsSince(
                userIds,
                System.currentTimeMillis() - FETCH_INTERVAL_MILLIS,
            )
        (userIds - fetchedUserIds.toSet()).let { ids ->
            if (ids.isNotEmpty()) {
                jobManager.addJobInBackground(RefreshUserJob(ids, forceRefresh = true))
            }
        }
    }
}
