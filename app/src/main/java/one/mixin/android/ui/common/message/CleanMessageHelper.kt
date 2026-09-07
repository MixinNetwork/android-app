package one.mixin.android.ui.common.message

import one.mixin.android.MixinApplication
import one.mixin.android.db.ConversationDao
import one.mixin.android.db.ConversationExtDao
import one.mixin.android.db.MessageDao
import one.mixin.android.db.MixinDatabase
import one.mixin.android.db.deleteMessageByIds
import one.mixin.android.db.withRoomTransaction
import one.mixin.android.db.flow.MessageFlow
import one.mixin.android.fts.FtsDatabase
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
        private val jobManager: MixinJobManager,
        private val appDatabase: MixinDatabase,
        private val messageDao: MessageDao,
        private val conversationDao: ConversationDao,
        private val conversationExtDao: ConversationExtDao,
        private val ftsDatabase: FtsDatabase,
    ) {
        suspend fun deleteMessageByConversationId(
            conversationId: String,
            deleteConversation: Boolean = false,
        ) {
            val lastRowId = appDatabase.withRoomTransaction {
                if (messageDao.findLastMessageId(conversationId) == null) {
                    if (deleteConversation) {
                        conversationDao.deleteConversationById(conversationId)
                        conversationExtDao.deleteConversationById(conversationId)
                    }
                    null
                } else {
                    messageDao.getLastMessageRowId()
                }
            } ?: return
            jobManager.addJobInBackground(
                MessageDeleteJob(conversationId, lastRowId, deleteConversation),
            )
        }

        fun deleteMessageItems(messageItems: List<MessageItem>) {
            messageItems.forEach { item ->
                item.absolutePath()?.takeIf { it.isNotBlank() && item.mediaStatus == MediaStatus.DONE.name }?.let {
                    jobManager.addJobInBackground(AttachmentDeleteJob(it))
                }
                if (item.isTranscript()) {
                    deleteTranscriptByMessageId(item.messageId)
                }
                jobManager.cancelJobByMixinJobId(item.messageId)
            }
            messageItems.groupBy { it.conversationId }.forEach { (conversationId, items) ->
                deleteMessages(conversationId, items.map { it.messageId })
            }
        }

        fun deleteMessageMinimals(
            conversationId: String,
            messageItems: List<MediaMessageMinimal>,
        ) {
            messageItems.forEach { item ->
                item.absolutePath(MixinApplication.appContext, conversationId, item.mediaUrl)?.takeIf { it.isNotBlank() }?.let {
                    jobManager.addJobInBackground(AttachmentDeleteJob(it))
                }
            }
            deleteMessages(conversationId, messageItems.map { it.messageId })
        }

        private fun deleteMessages(conversationId: String, messageIds: List<String>) {
            messageIds.chunked(500).forEach { ids ->
                appDatabase.deleteMessageByIds(ids)
                ftsDatabase.deleteByMessageIds(ids)
                MessageFlow.delete(conversationId, ids)
            }
        }

        private fun deleteTranscriptByMessageId(messageId: String) {
            jobManager.addJobInBackground(TranscriptDeleteJob(listOf(messageId)))
        }
    }
