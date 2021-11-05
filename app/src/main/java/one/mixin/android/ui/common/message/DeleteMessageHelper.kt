package one.mixin.android.ui.common.message

import one.mixin.android.db.ConversationDao
import one.mixin.android.db.MessageDao
import one.mixin.android.job.AttachmentDeleteJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.MediaMessageDeleteJob
import one.mixin.android.repository.ConversationRepository
import javax.inject.Inject

class DeleteMessageHelper @Inject internal constructor(
    private val jobManager: MixinJobManager,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    suspend fun deleteMessageByConversationId(conversationId: String) {
    }

    suspend fun deleteMessageByCategory() {
    }

    suspend fun deleteMessageByMessageId(messageId: String) {
        messageDao.getMediaMessageMinimalById(messageId)?.let { mediaMessageMinimal ->
            jobManager.addJob(MediaMessageDeleteJob(listOf(mediaMessageMinimal)))
        }
    }
}