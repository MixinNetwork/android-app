package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.Account.PREF_FTS4_REDUCE
import one.mixin.android.util.PropertyHelper
import timber.log.Timber

class ReduceFts4Job :
    BaseJob(Params(PRIORITY_UI_HIGH).groupBy(GROUP_ID).persist()) {
    companion object {
        private const val GROUP_ID = "ReduceFts4Job"
    }

    override fun onRun() = runBlocking {
        PropertyHelper.updateKeyValue(PREF_FTS4_REDUCE, false.toString())
        Timber.e("Delete message fts4 completed!!!")
    }
}
