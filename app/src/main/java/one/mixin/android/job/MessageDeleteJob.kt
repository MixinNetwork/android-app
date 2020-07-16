package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants.DB_DELETE_LIMIT

class MessageDeleteJob(private val conversationId: String, private val deleteMention: Boolean = false, private val deleteConversation: Boolean = false) :
    BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).groupBy("message_delete").persist()) {

    private val TAG = MessageDeleteJob::class.java.simpleName

    companion object {
        const val GROUP = "MessageDeleteJob"
        private const val serialVersionUID = 1L
    }

    override fun onRun() = runBlocking {
        if (deleteMention) {
            repeat(messageMentionDao.countDeleteMessageByConversationId(conversationId) / DB_DELETE_LIMIT + 1) {
                messageMentionDao.deleteMessageByConversationId(conversationId, DB_DELETE_LIMIT)
            }
        } else {
            repeat(messageDao.countDeleteMessageByConversationId(conversationId) / DB_DELETE_LIMIT + 1) {
                messageFts4Dao.deleteMessageByConversationId(conversationId, DB_DELETE_LIMIT)
                messageDao.deleteMessageByConversationId(conversationId, DB_DELETE_LIMIT)
            }
        }
        if (deleteConversation) {
            conversationDao.deleteConversationById(conversationId)
        }
    }
}
