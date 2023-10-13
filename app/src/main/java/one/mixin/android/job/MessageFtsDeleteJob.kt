package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking

@Deprecated("Deprecated in favor of JobManager", replaceWith = ReplaceWith("FtsDbHelper"))
class MessageFtsDeleteJob(private val messageIds: List<String>) :
    BaseJob(Params(PRIORITY_LOWER).addTags(GROUP).groupBy("message_delete").persist()) {

    companion object {
        const val GROUP = "MessageFtsDeleteJob"
        private const val serialVersionUID = 1L
    }

    override fun onRun() = runBlocking {
    }
}
