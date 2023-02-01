package one.mixin.android.ui.common.message

import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.db.insertNoReplace
import one.mixin.android.db.runInTransaction
import one.mixin.android.di.ApplicationScope
import one.mixin.android.util.SINGLE_THREAD
import one.mixin.android.util.debug.timeoutEarlyWarning
import one.mixin.android.vo.MessageMentionStatus
import one.mixin.android.vo.createAckJob
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.CREATE_MESSAGE
import javax.inject.Inject

class ChatRoomHelper @Inject internal constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val conversationDao: ConversationDao,
    private val remoteMessageStatusDao: RemoteMessageStatusDao,
    private val messageMentionDao: MessageMentionDao,
    private val jobDao: JobDao,
) {
    fun saveDraft(conversationId: String, draft: String) = applicationScope.launch {
        timeoutEarlyWarning({
            val localDraft = conversationDao.getConversationDraftById(conversationId)
            if (localDraft != draft) {
                conversationDao.saveDraft(conversationId, draft)
            }
        })
    }

    fun markMessageRead(conversationId: String) {
        applicationScope.launch(SINGLE_THREAD) {
            timeoutEarlyWarning({
                runInTransaction {
                    remoteMessageStatusDao.rawUpdateQuery(SimpleSQLiteQuery("UPDATE remote_messages_status SET status = 'READ' WHERE conversation_id = '$conversationId'"))
                    remoteMessageStatusDao.rawUpdateQuery(SimpleSQLiteQuery("UPDATE conversations SET unseen_message_count = 0 WHERE conversation_id = '$conversationId'"))
                }
            })
        }
    }

    fun markMentionRead(messageId: String, conversationId: String) {
        applicationScope.launch {
            messageMentionDao.suspendMarkMentionRead(messageId)
            jobDao.insertNoReplace(createAckJob(CREATE_MESSAGE, BlazeAckMessage(messageId, MessageMentionStatus.MENTION_READ.name), conversationId))
        }
    }
}
