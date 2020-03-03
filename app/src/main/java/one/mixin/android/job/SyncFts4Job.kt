package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.util.MessageFts4Helper

class SyncFts4Job : BaseJob(Params(PRIORITY_SYNC_FTS).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val GROUP_ID = "sync_fts_group"
    }

    override fun onRun() = runBlocking {
        val done = MessageFts4Helper.syncMessageFts4(
            preProcess = false,
            waitMillis = 1000L
        )
        if (!done) {
            jobManager.addJobInBackground(SyncFts4Job())
        }
        return@runBlocking
    }
}
