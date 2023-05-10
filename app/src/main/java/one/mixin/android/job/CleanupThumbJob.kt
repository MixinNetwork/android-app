package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.db.property.PropertyHelper

class CleanupThumbJob(private val lastRowId:Long = -1) : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "CleanupThumbJob"
    }

    override fun onRun() = runBlocking {
        // Because job must be recursively completed, set the flag
        PropertyHelper.updateKeyValue(Constants.Account.PREF_CLEANUP_THUMB, false)
        val list = messageDao.findBigThumb(lastRowId, 100)
        if (list.isNotEmpty()) {
            messageDao.cleanupBigThumb(list)
        }
        if (list.size >= 100) {
            jobManager.addJobInBackground(CleanupThumbJob(list.last()))
        }
    }
}
