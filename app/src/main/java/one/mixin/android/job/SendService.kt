package one.mixin.android.job

import android.app.IntentService
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.core.content.getSystemService
import dagger.android.AndroidInjection
import java.util.UUID
import javax.inject.Inject
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.batchMarkReadAndTake
import one.mixin.android.di.type.DatabaseCategory
import one.mixin.android.di.type.DatabaseCategoryEnum
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.NotificationJob.Companion.CONVERSATION_ID
import one.mixin.android.job.NotificationJob.Companion.IS_PLAIN
import one.mixin.android.job.NotificationJob.Companion.KEY_REPLY
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createAckJob
import one.mixin.android.vo.createMessage
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.CREATE_MESSAGE

class SendService : IntentService("SendService") {

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    @field:[DatabaseCategory(DatabaseCategoryEnum.BASE)]
    lateinit var messageDao: MessageDao
    @Inject
    lateinit var messageMentionDao: MessageMentionDao
    @Inject
    lateinit var jobDao: JobDao

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val bundle = RemoteInput.getResultsFromIntent(intent)
        val conversationId = intent.getStringExtra(CONVERSATION_ID) ?: return
        if (bundle != null) {
            val content = bundle.getCharSequence(KEY_REPLY) ?: return
            val category = if (intent.getBooleanExtra(IS_PLAIN, false)) {
                MessageCategory.PLAIN_TEXT.name
            } else {
                MessageCategory.SIGNAL_TEXT.name
            }

            val message = createMessage(UUID.randomUUID().toString(), conversationId,
                Session.getAccountId().toString(), category, content.toString().trim(), nowInUtc(), MessageStatus.SENDING.name)
            jobManager.addJobInBackground(SendMessageJob(message))
        }
        val manager = getSystemService<NotificationManager>()
        manager?.cancel(conversationId.hashCode())
        messageMentionDao.markMentionReadByConversationId(conversationId)
        messageDao.findUnreadMessagesSync(conversationId)?.let { list ->
            if (list.isNotEmpty()) {
                messageDao.batchMarkReadAndTake(conversationId, Session.getAccountId()!!, list.last().createdAt)
                list.map { BlazeAckMessage(it.id, MessageStatus.READ.name) }.let { messages ->
                    val chunkList = messages.chunked(100)
                    for (item in chunkList) {
                        jobManager.addJobInBackground(SendAckMessageJob(item))
                    }
                }
                Session.getExtensionSessionId()?.let {
                    list.map { createAckJob(CREATE_MESSAGE, BlazeAckMessage(it.id, MessageStatus.READ.name), conversationId) }.let {
                        jobDao.insertList(it)
                    }
                }
            }
        }
    }
}
