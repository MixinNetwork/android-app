package one.mixin.android.job

import com.birbit.android.jobqueue.Params

class FtsDeleteJob(private val messageId: String) : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).groupBy("fts_delete").persist()) {

    companion object {
        const val GROUP = "FtsDeleteJob"
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
    }
}
