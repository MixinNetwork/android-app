package one.mixin.android.job

import android.app.IntentService
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.RemoteInput
import androidx.core.content.getSystemService
import dagger.hilt.android.AndroidEntryPoint
import one.mixin.android.db.JobDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MessageMentionDao
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.db.runInTransaction
import one.mixin.android.extension.nowInUtc
import one.mixin.android.session.Session
import one.mixin.android.vo.EncryptCategory
import one.mixin.android.vo.MessageCategory
import one.mixin.android.vo.MessageStatus
import one.mixin.android.vo.createMessage
import one.mixin.android.vo.toCategory
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SendService : IntentService("SendService") {

    @Inject
    lateinit var jobManager: MixinJobManager
    @Inject
    lateinit var messageDao: MessageDao
    @Inject
    lateinit var remoteMessageStatusDao: RemoteMessageStatusDao
    @Inject
    lateinit var messageMentionDao: MessageMentionDao
    @Inject
    lateinit var jobDao: JobDao

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val bundle = RemoteInput.getResultsFromIntent(intent)
        val conversationId = intent.getStringExtra(CONVERSATION_ID) ?: return
        if (bundle != null) {
            val content = bundle.getCharSequence(KEY_REPLY) ?: return
            val encryptCategory = EncryptCategory.values()[intent.getIntExtra(ENCRYPTED_CATEGORY, EncryptCategory.PLAIN.ordinal)]
            val category = encryptCategory.toCategory(
                MessageCategory.PLAIN_TEXT,
                MessageCategory.SIGNAL_TEXT,
                MessageCategory.ENCRYPTED_TEXT,
            )

            val message = createMessage(
                UUID.randomUUID().toString(),
                conversationId,
                Session.getAccountId().toString(),
                category,
                content.toString().trim(),
                nowInUtc(),
                MessageStatus.SENDING.name
            )
            jobManager.addJobInBackground(SendMessageJob(message))
        }
        val manager = getSystemService<NotificationManager>()
        manager?.cancel(conversationId.hashCode())
        messageMentionDao.markMentionReadByConversationId(conversationId)
        runInTransaction {
            remoteMessageStatusDao.markReadByConversationId(conversationId)
            remoteMessageStatusDao.updateConversationUnseen(conversationId)
        }
    }
}
