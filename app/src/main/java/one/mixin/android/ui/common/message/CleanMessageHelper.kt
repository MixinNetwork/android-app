package one.mixin.android.ui.common.message

import one.mixin.android.Constants.DB_DELETE_LIMIT
import one.mixin.android.Constants.DB_DELETE_MEDIA_LIMIT
import one.mixin.android.MixinApplication
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ConversationExtDao
import one.mixin.android.db.DatabaseProvider
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.RemoteMessageStatusDao
import one.mixin.android.db.deleteMessageById
import one.mixin.android.db.deleteMessageByIds
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.fts.FtsDatabase
import one.mixin.android.fts.deleteByMessageId
import one.mixin.android.fts.deleteByMessageIds
import one.mixin.android.job.AttachmentDeleteJob
import one.mixin.android.job.MessageDeleteJob
import one.mixin.android.job.MixinJobManager
import one.mixin.android.job.TranscriptDeleteJob
import one.mixin.android.vo.MediaMessageMinimal
import one.mixin.android.vo.MediaStatus
import one.mixin.android.vo.MessageItem
import one.mixin.android.vo.absolutePath
import one.mixin.android.vo.isTranscript
import javax.inject.Inject

class CleanMessageHelper
    @Inject
    internal constructor(
        private val databaseProvider: DatabaseProvider,
        private val jobManager: MixinJobManager,
        private val messageDao: MessageDao,
        private val conversationDao: ConversationDao,
        private val remoteMessageStatusDao: RemoteMessageStatusDao,
        private val conversationExtDao: ConversationExtDao,
    ) {
        private val ftsDatabase: FtsDatabase
            get() = databaseProvider.getFtsDatabase()
        private val appDatabase: MixinDatabase
            get() = databaseProvider.getMixinDatabase()

        suspend fun deleteMessageByConversationId(
            conversationId: String,
            deleteConversation: Boolean = false,
        ) {
            // DELETE message's media
            var repeatTimes = 0
            var messageList: List<MediaMessageMinimal>
            do {
                messageList =
                    messageDao.getMediaMessageMinimalByConversationId(
                        conversationId,
                        DB_DELETE_MEDIA_LIMIT,
                        DB_DELETE_MEDIA_LIMIT * repeatTimes++,
                    )
                messageList.mapNotNull {
                    it.absolutePath(MixinApplication.appContext, conversationId, it.mediaUrl)
                }.apply {
                    jobManager.addJobInBackground(AttachmentDeleteJob(*this.toTypedArray()))
                }
            } while (messageList.size > DB_DELETE_MEDIA_LIMIT)
            // DELETE transcript message
            var messageIds: List<String>
            repeatTimes = 0
            do {
                messageIds =
                    messageDao.getTranscriptMessageIdByConversationId(
                        conversationId,
                        DB_DELETE_LIMIT,
                        DB_DELETE_LIMIT * repeatTimes++,
                    )
                jobManager.addJobInBackground(TranscriptDeleteJob(messageIds))
            } while (messageIds.size > DB_DELETE_LIMIT)

            // DELETE message
            val deleteCount = messageDao.countDeleteMessageByConversationId(conversationId)
            if (deleteCount <= 0) {
                if (deleteConversation) {
                    conversationDao.deleteConversationById(conversationId)
                    conversationExtDao.deleteConversationById(conversationId)
                    // No message data, no notification
                }
            } else {
                val lastRowId = messageDao.findLastMessageRowId(conversationId) ?: return
                if (deleteCount > DB_DELETE_LIMIT) {
                    jobManager.addJobInBackground(
                        MessageDeleteJob(
                            conversationId,
                            lastRowId,
                            deleteConversation = deleteConversation,
                        ),
                    )
                } else {
                    val ids =
                        messageDao
                            .getMessageIdsByConversationId(conversationId, lastRowId)
                    ftsDatabase.deleteByMessageIds(ids)
                    appDatabase.deleteMessageByIds(ids)
                    if (deleteConversation) {
                        conversationDao.deleteConversationById(conversationId)
                        conversationExtDao.deleteConversationById(conversationId)
                    } else {
                        remoteMessageStatusDao.countUnread(conversationId)
                        conversationDao.refreshLastMessageId(conversationId)
                        conversationExtDao.refreshCountByConversationId(conversationId)
                    }
                    MessageFlow.delete(conversationId, ids)
                }
            }
        }

        fun deleteMessageItems(messageItems: List<MessageItem>) {
            messageItems.forEach { item ->
                deleteMessage(
                    item.messageId,
                    item.conversationId,
                    item.absolutePath(),
                    item.mediaStatus == MediaStatus.DONE.name,
                )

                if (item.isTranscript()) {
                    deleteTranscriptByMessageId(item.messageId)
                }
                jobManager.cancelJobByMixinJobId(item.messageId)
            }
        }

        fun deleteMessageMinimals(
            conversationId: String,
            messageItems: List<MediaMessageMinimal>,
        ) {
            messageItems.forEach { item ->
                deleteMessage(
                    item.messageId,
                    conversationId,
                    item.absolutePath(MixinApplication.appContext, conversationId, item.mediaUrl),
                )
            }
        }

        private fun deleteMessage(
            messageId: String,
            conversationId: String,
            mediaUrl: String? = null,
            forceDelete: Boolean = true,
        ) {
            if (!mediaUrl.isNullOrBlank() && forceDelete) {
                jobManager.addJobInBackground(AttachmentDeleteJob(mediaUrl))
            }
            appDatabase.deleteMessageById(messageId, conversationId)
            ftsDatabase.deleteByMessageId(messageId)
            MessageFlow.delete(conversationId, messageId)
        }

        private fun deleteTranscriptByMessageId(messageId: String) {
            jobManager.addJobInBackground(TranscriptDeleteJob(listOf(messageId)))
        }
    }
