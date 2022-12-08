package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.deleteFtsByMessageIds

class MessageFtsDeleteJob(private val messageIds: List<String>) :
    BaseJob(Params(PRIORITY_LOWER).addTags(GROUP).groupBy("message_delete").persist()) {

    private val TAG = MessageFtsDeleteJob::class.java.simpleName

    companion object {
        const val GROUP = "MessageFtsDeleteJob"
        private const val serialVersionUID = 1L
    }

    override fun onRun() = runBlocking {
        messageIds.chunked(100).forEach {
            deleteFtsByMessageIds(it)
        }
    }
}
