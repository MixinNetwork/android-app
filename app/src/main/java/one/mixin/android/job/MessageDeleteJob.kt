package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.DB_DELETE_LIMIT
import one.mixin.android.db.deleteMessageByIds
import one.mixin.android.util.chat.InvalidateFlow

class MessageDeleteJob(
    private val conversationId: String,
    private val lastRowId: Long,
    private val deleteConversation:Boolean
) :
    BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).groupBy("message_delete").persist()) {

    private val TAG = MessageDeleteJob::class.java.simpleName

    companion object {
        const val GROUP = "MessageDeleteJob"
        private const val serialVersionUID = 2L
    }

    override fun onRun() = runBlocking {
        val deleteTimes =
            messageDao.countDeleteMessageByConversationId(conversationId) / DB_DELETE_LIMIT + 1
        repeat(deleteTimes) {
            val ids = messageDao.getMessageIdsByConversationId(
                conversationId,
                lastRowId,
                DB_DELETE_LIMIT
            )
            jobManager.addJobInBackground(MessageFtsDeleteJob(ids))
            appDatabase.deleteMessageByIds(ids)
        }
        if (deleteConversation && messageDao.findLastMessageRowId(conversationId) == lastRowId) {
            conversationDao.deleteConversationById(conversationId)
        }
        InvalidateFlow.emit(conversationId)
    }
}
