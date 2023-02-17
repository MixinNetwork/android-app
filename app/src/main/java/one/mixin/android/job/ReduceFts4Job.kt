package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking

class ReduceFts4Job :
    BaseJob(Params(PRIORITY_UI_HIGH).groupBy(GROUP_ID).persist()) {
    companion object {
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "ReduceFts4Job"
    }

    override fun onRun() = runBlocking {
    }
}
