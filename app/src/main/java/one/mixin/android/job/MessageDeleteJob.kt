package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import one.mixin.android.Constants.DB_DELETE_LIMIT
import one.mixin.android.MixinApplication
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isTranscript
import one.mixin.android.db.deleteMessageByIds
import one.mixin.android.db.withRoomTransaction
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.fts.deleteByMessageIds

class MessageDeleteJob(
    private val conversationId: String,
    private val lastRowId: Long,
    private val deleteConversation: Boolean,
) :
    BaseJob(Params(PRIORITY_UI_HIGH).addTags(GROUP).groupBy("message_delete").persist()) {
    private val TAG = MessageDeleteJob::class.java.simpleName

    companion object {
        const val GROUP = "MessageDeleteJob"
        private const val serialVersionUID = 2L
    }

    override fun onRun() =
        runBlocking {
            while (true) {
                val ids =
                    messageDao.getMessageIdsByConversationId(
                        conversationId,
                        lastRowId,
                        DB_DELETE_LIMIT,
                    )
                if (ids.isEmpty()) break
                val cleanup = messageDao.getMessagesForDeletion(ids)
                val paths = cleanup.mapNotNull {
                    it.absolutePath(MixinApplication.appContext, conversationId, it.mediaUrl)
                }
                if (paths.isNotEmpty()) {
                    jobManager.addJobInBackground(AttachmentDeleteJob(*paths.toTypedArray()))
                }
                val transcripts = cleanup.filter { it.isTranscript() }.map { it.messageId }
                if (transcripts.isNotEmpty()) {
                    jobManager.addJobInBackground(TranscriptDeleteJob(transcripts))
                }
                ftsDatabase.deleteByMessageIds(ids)
                appDatabase.deleteMessageByIds(ids)
                MessageFlow.delete(conversationId, ids)
                yield()
            }
            if (deleteConversation) {
                appDatabase.withRoomTransaction {
                    if (messageDao.findLastMessageId(conversationId) == null) {
                        conversationDao.deleteConversationById(conversationId)
                        conversationExtDao.deleteConversationById(conversationId)
                    }
                }
            }
        }
}
