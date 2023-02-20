package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.util.MessageFts4Helper

class SyncFts4Job : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "sync_fts_group"
    }

    override fun onRun() = runBlocking {
        return@runBlocking
    }
}
