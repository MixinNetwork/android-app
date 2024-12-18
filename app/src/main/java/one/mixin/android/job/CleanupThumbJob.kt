package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.db.property.PropertyHelper

class CleanupThumbJob : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "CleanupThumbJob"
    }

    override fun onRun() =
        runBlocking {
            messageDao().cleanupBigThumb()
            PropertyHelper.updateKeyValue(Constants.Account.PREF_CLEANUP_THUMB, false)
        }
}
