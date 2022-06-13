package one.mixin.android.ui.common.message

import one.mixin.android.Constants.DB_DELETE_LIMIT
import one.mixin.android.Constants.DB_DELETE_MEDIA_THRESHOLD
import one.mixin.android.Constants.DB_DELETE_THRESHOLD
import one.mixin.android.MixinApplication
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.deleteMessageById
import one.mixin.android.db.deleteMessageByIds
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

class CleanMessageHelper @Inject internal constructor(private val jobManager: MixinJobManager, private val appDatabase: MixinDatabase) {

    suspend fun deleteMessageByConversationId(conversationId: String, deleteConversation: Boolean = false) {
        // DELETE message's media
        var repeatTimes = 0
        var messageList: List<MediaMessageMinimal>
        do {
            messageList = appDatabase.messageDao().getMediaMessageMinimalByConversationId(
                conversationId,
                DB_DELETE_MEDIA_THRESHOLD, DB_DELETE_MEDIA_THRESHOLD * repeatTimes++
            )
            messageList.mapNotNull {
                it.absolutePath(MixinApplication.appContext, conversationId, it.mediaUrl)
            }.apply {
                jobManager.addJobInBackground(AttachmentDeleteJob(*this.toTypedArray()))
            }
        } while (messageList.size > DB_DELETE_MEDIA_THRESHOLD)
        // DELETE transcript message
        var messageIds: List<String>
        repeatTimes = 0
        do {
            messageIds = appDatabase.messageDao().getTranscriptMessageIdByConversationId(
                conversationId,
                DB_DELETE_LIMIT, DB_DELETE_LIMIT * repeatTimes++
            )
            jobManager.addJobInBackground(TranscriptDeleteJob(messageIds))
        } while (messageIds.size > DB_DELETE_MEDIA_THRESHOLD)

        // DELETE message
        val deleteCount = appDatabase.messageDao().countDeleteMessageByConversationId(conversationId)
        val lastRowId = appDatabase.messageDao().findLastMessageRowId(conversationId) ?: return
        if (deleteCount > DB_DELETE_THRESHOLD) {
            jobManager.addJobInBackground(MessageDeleteJob(conversationId, lastRowId, deleteConversation = deleteConversation))
        } else {
            val ids = appDatabase.messageDao().getMessageIdsByConversationId(conversationId, lastRowId)
            jobManager.addJobInBackground(MessageFtsDeleteJob(ids))
            appDatabase.deleteMessageByIds(ids)
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
