package one.mixin.android.job

import android.app.IntentService
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.core.content.getSystemService
import dagger.android.AndroidInjection
import one.mixin.android.db.MessageDao
import one.mixin.android.db.batchMarkReadAndTake
import one.mixin.android.extension.nowInUtc
import one.mixin.android.job.NotificationJob.Companion.CONVERSATION_ID
import one.mixin.android.job.NotificationJob.Companion.IS_PLAIN
import one.mixin.android.job.NotificationJob.Companion.KEY_REPLY
import one.mixin.android.util.Session
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createMessage
import one.mixin.android.websocket.BlazeAckMessage
import one.mixin.android.websocket.createAckListParamBlazeMessage
import java.util.UUID
import javax.inject.Inject

class SendService : IntentService("SendService") {

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var messageDao: MessageDao

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
    }

    override fun onHandleIntent(intent: Intent) {
        val bundle = RemoteInput.getResultsFromIntent(intent)
        if (bundle != null) {
            val content = bundle.getCharSequence(KEY_REPLY)
            val conversationId = intent.getStringExtra(CONVERSATION_ID)
            val category = if (intent.getBooleanExtra(IS_PLAIN, false)) {
                MessageCategory.PLAIN_TEXT.name
            } else {
                MessageCategory.SIGNAL_TEXT.name
            }
            val manager = getSystemService<NotificationManager>()
            manager?.cancel(conversationId.hashCode())
            val message = createMessage(UUID.randomUUID().toString(), conversationId,
                Session.getAccountId().toString(), category, content.toString().trim(), nowInUtc(), MessageStatus.SENDING)
            jobManager.addJobInBackground(SendMessageJob(message))
            messageDao.findUnreadMessagesSync(conversationId)?.let {
                if (it.isNotEmpty()) {
                    messageDao.batchMarkReadAndTake(conversationId, Session.getAccountId()!!, it.last().created_at)
                    it.map { BlazeAckMessage(it.id, MessageStatus.READ.name) }.let {
                        val chunkList = it.chunked(100)
                        for (item in chunkList) {
                            jobManager.addJobInBackground(SendAckMessageJob(createAckListParamBlazeMessage(item)))
                        }
                    }
                }
            }
        }
    }
}
