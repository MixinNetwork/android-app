package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import one.mixin.android.db.deleteFtsByMessageId

class FtsDeleteJob(private val messageId: String) : BaseJob(Params(PRIORITY_BACKGROUND).addTags(GROUP).groupBy("fts_delete").persist()) {

    private val TAG = FtsDeleteJob::class.java.simpleName

    companion object {
        const val GROUP = "FtsDeleteJob"
        private const val serialVersionUID = 1L
    }

    override fun onRun() {
        messageFts4Dao.deleteFtsByMessageId(messageId)
    }
}
