package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.PREF_FTS4_REDUCE
import one.mixin.android.Constants.DB_DELETE_LIMIT
import one.mixin.android.util.PropertyHelper
import timber.log.Timber

class ReduceFts4Job :
    BaseJob(Params(PRIORITY_UI_HIGH).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val GROUP_ID = "ReduceFts4Job"
    }

    override fun onRun() = runBlocking {
        val list = messageFts4Dao.excludeIds(DB_DELETE_LIMIT)
        delete(list)
        if (list.size < DB_DELETE_LIMIT) {
            PropertyHelper.updateKeyValue(PREF_FTS4_REDUCE, false.toString())
            Timber.e("Delete message fts4 completed!!!")
        } else {
            PropertyHelper.updateKeyValue(PREF_FTS4_REDUCE, true.toString())
            jobManager.addJobInBackground(ReduceFts4Job())
        }
    }

    private suspend fun delete(list: List<String>) {
        if (list.isEmpty()) return
        val startTime = System.currentTimeMillis()
        messageFts4Dao.deleteByMessageIds(list)
        Timber.e("Delete message fts4 ${list.size}, cost: ${System.currentTimeMillis() - startTime} ms")
    }
}
