package one.mixin.android.ui.common.message

import one.mixin.android.Constants.DB_DELETE_LIMIT
import one.mixin.android.Constants.DB_DELETE_THRESHOLD
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.deleteMessageByConversationId
import one.mixin.android.db.deleteMessageById
import one.mixin.android.job.AttachmentDeleteJob
import one.mixin.android.job.FtsDeleteJob
import one.mixin.android.job.MessageDeleteJob
import one.mixin.android.job.MessageFtsDeleteJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.TranscriptDeleteJob
import one.mixin.android.util.chat.InvalidateFlow
import one.mixin.android.vo.MediaMessageMinimal
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isTranscript
import javax.inject.Inject

class CleanMessageHelper @Inject internal constructor(private val jobManager: MixinJobManager,private val appDatabase:MixinDatabase) {

    // Todo optimize
    suspend fun deleteMessageByConversationId(conversationId: String, deleteConversation:Boolean = false) {
        appDatabase.messageDao().findAllMediaPathByConversationId(conversationId).let { list ->
            if (list.isNotEmpty()) {
                jobManager.addJobInBackground(AttachmentDeleteJob(* list.toTypedArray()))
            }
        }
        val deleteMentionCount = appDatabase.mentionMessageDao().countDeleteMessageByConversationId(conversationId)
        if (deleteMentionCount > DB_DELETE_THRESHOLD) {
            jobManager.addJobInBackground(MessageDeleteJob(conversationId, true))
        } else {
            val deleteTimes = deleteMentionCount / DB_DELETE_LIMIT + 1
            repeat(deleteTimes) {
                appDatabase.mentionMessageDao().deleteMessageByConversationId(conversationId, DB_DELETE_LIMIT)
            }
        }
        val deleteCount = appDatabase.messageDao().countDeleteMessageByConversationId(conversationId)
        if (deleteCount > DB_DELETE_THRESHOLD) {
            jobManager.addJobInBackground(MessageDeleteJob(conversationId, deleteConversation = deleteConversation))
        } else {
            val deleteTimes = deleteCount / DB_DELETE_LIMIT + 1
            jobManager.addJobInBackground(
                MessageFtsDeleteJob(
                    appDatabase.messageDao().getMessageIdsByConversationId(
                        conversationId
                    )
                )
            )
            repeat(deleteTimes) {
                if (!deleteConversation) {
                    appDatabase.deleteMessageByConversationId(conversationId, DB_DELETE_LIMIT)
                }
            }
            if (deleteConversation) {
                appDatabase.conversationDao().deleteConversationById(conversationId)
            }
            InvalidateFlow.emit(conversationId)
        }
    }

    suspend fun deleteMessageItems(messageItems: List<MessageItem>) {
        messageItems.forEach { item ->
            deleteMessage(
                item.messageId,
                item.conversationId,
                item.absolutePath(),
                item.mediaStatus == MediaStatus.DONE.name
            )

            if (item.isTranscript()) {
                deleteTranscriptByMessageId(item.messageId)
            }
            jobManager.cancelJobByMixinJobId(item.messageId)
        }
    }

    suspend fun deleteMessageMinimals(
        conversationId: String,
        messageItems: List<MediaMessageMinimal>
    ) {
        messageItems.forEach { item ->
            deleteMessage(
                item.messageId,
                conversationId,
                item.absolutePath(MixinApplication.appContext, conversationId, item.mediaUrl)
            )
        }
    }

    private fun deleteMessage(messageId: String, conversationId: String, mediaUrl: String? = null, forceDelete: Boolean = true) {
        if (!mediaUrl.isNullOrBlank() && forceDelete) {
            jobManager.addJobInBackground(AttachmentDeleteJob(mediaUrl))
        }
        appDatabase.deleteMessageById(messageId)
        jobManager.addJobInBackground(FtsDeleteJob(messageId))
        InvalidateFlow.emit(conversationId)
    }

    private fun deleteTranscriptByMessageId(messageId: String) {
        jobManager.addJobInBackground(TranscriptDeleteJob(listOf(messageId)))
    }
}
