package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.util.SINGLE_DB_THREAD
import timber.log.Timber

class ClearFts4Job :
    BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "ClearFts4Job"
        const val FTS_CLEAR = "fts_clear"
    }

    override fun onRun() =
        runBlocking(SINGLE_DB_THREAD) {
            val count = messageDao().deleteFts()
            if (count > 0) {
                jobManager.addJobInBackground(ClearFts4Job())
            } else {
                PropertyHelper.updateKeyValue(FTS_CLEAR, false)
            }
            Timber.e("DELETE fts count: $count")
            return@runBlocking
        }
}
