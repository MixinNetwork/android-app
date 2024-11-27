package one.mixin.android.job

import com.birbit.android.jobqueue.Params
import kotlinx.coroutines.runBlocking
import one.mixin.android.db.property.PropertyHelper
import one.mixin.android.extension.joinWhiteSpace
import one.mixin.android.fts.insertFts4
import one.mixin.android.fts.insertOrReplaceMessageFts4
import one.mixin.android.vo.isContact
import one.mixin.android.vo.isData
import one.mixin.android.vo.isPost
import one.mixin.android.vo.isText
import one.mixin.android.vo.isTranscript
import timber.log.Timber

class MigratedFts4Job : BaseJob(Params(PRIORITY_LOWER).groupBy(GROUP_ID).persist()) {
    companion object {
        const val FTS_NEED_MIGRATED_LAST_ROW_ID = "fts_need_migrated_last_row_id"
        private const val MIGRATED_LIMIT = 1000
        private var serialVersionUID: Long = 1L
        private const val GROUP_ID = "MigratedFts4Job"
    }

    override fun onRun() =
        runBlocking {
            var lastRowId = propertyDao().findValueByKey(FTS_NEED_MIGRATED_LAST_ROW_ID)?.toLongOrNull() ?: 0L
            if (lastRowId == -1L) return@runBlocking
            if (lastRowId == 0L) { // Get the rowid of the current last message
                val currentLastId = messageDao().getLastMessageRowId()
                if (currentLastId == null) { // No data, no migration required
                    PropertyHelper.updateKeyValue(FTS_NEED_MIGRATED_LAST_ROW_ID, -1)
                    return@runBlocking
                } else {
                    lastRowId = currentLastId + 1 // It is easy to obtain data that is less than or equal to it.
                }
            }
            val messages = messageDao().findFtsMessages(lastRowId, MIGRATED_LIMIT)
            messages.forEach { message ->
                if (message.isTranscript()) {
                    val content =
                        transcriptMessageDao().getTranscriptsById(message.messageId).filter { it.isText() || it.isPost() || it.isData() || it.isContact() }
                            .map { transcript ->
                                if (transcript.isData()) {
                                    transcript.mediaName
                                } else {
                                    if (transcript.isContact()) {
                                        transcript.sharedUserId?.let { userId -> userDao().findUser(userId) }?.fullName
                                    } else {
                                        transcript.content
                                    }
                                }
                            }.filter { it.isNullOrBlank().not() }.joinToString("").joinWhiteSpace()
                    ftsDatabase().insertFts4(content.joinWhiteSpace(), message.conversationId, message.messageId, message.category, message.userId, message.createdAt)
                } else {
                    ftsDatabase().insertOrReplaceMessageFts4(message)
                }
            }
            if (messages.size < MIGRATED_LIMIT) {
                PropertyHelper.updateKeyValue(FTS_NEED_MIGRATED_LAST_ROW_ID, -1)
                PropertyHelper.updateKeyValue(ClearFts4Job.FTS_CLEAR, true)
            } else {
                lastRowId = messageDao().getMessageRowid(messages.last().messageId) ?: lastRowId
                PropertyHelper.updateKeyValue(FTS_NEED_MIGRATED_LAST_ROW_ID, lastRowId)
                jobManager.addJobInBackground(MigratedFts4Job())
            }
            Timber.e("Migrated size:${messages.size} - last id:$lastRowId")
            return@runBlocking
        }
}
